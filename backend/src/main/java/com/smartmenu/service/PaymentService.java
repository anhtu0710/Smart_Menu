package com.smartmenu.service;

import com.smartmenu.dto.AccessCheckResponseDto;
import com.smartmenu.dto.PaymentCreateResponseDto;
import com.smartmenu.entity.MenuGeneration;
import com.smartmenu.entity.Payment;
import com.smartmenu.entity.User;
import com.smartmenu.repository.MenuGenerationRepository;
import com.smartmenu.repository.PaymentRepository;
import com.smartmenu.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final MenuGenerationRepository menuGenerationRepository;

    private static final BigDecimal DEFAULT_AMOUNT = new BigDecimal("50000.00");
    private static final String DEFAULT_QR_IMAGE = "images/payment_qr.png";

    /**
     * BR01 & BR02: Kiểm tra quyền sử dụng dịch vụ thiết kế menu
     */
    public AccessCheckResponseDto checkAccess(Long userId) {
        if (userId == null) {
            return AccessCheckResponseDto.builder()
                    .allowed(false)
                    .type("UNAUTHORIZED")
                    .message("Vui lòng đăng nhập để sử dụng dịch vụ")
                    .build();
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return AccessCheckResponseDto.builder()
                    .allowed(false)
                    .type("USER_NOT_FOUND")
                    .message("Không tìm thấy thông tin tài khoản")
                    .build();
        }

        // Trường hợp 1: Người dùng chưa sử dụng bản miễn phí (free_used = 0)
        if (user.getFreeUsed() == null || !user.getFreeUsed()) {
            log.info("[ACCESS-CHECK] User ID={} chưa dùng thử (free_used=false) -> Cho phép dùng FREE", userId);
            return AccessCheckResponseDto.builder()
                    .allowed(true)
                    .type("FREE")
                    .message("Bạn có 01 lượt dùng thử miễn phí")
                    .build();
        }

        // Trường hợp 2: Người dùng đã dùng bản miễn phí, kiểm tra xem có giao dịch SUCCESS chưa tiêu thụ (consumed = 0)
        Optional<Payment> activePaid = paymentRepository.findFirstByUserIdAndStatusAndConsumedFalseOrderByCreatedDateAsc(userId, "SUCCESS");
        if (activePaid.isPresent()) {
            Payment p = activePaid.get();
            log.info("[ACCESS-CHECK] User ID={} đã thanh toán thành công Payment ID={} (consumed=false) -> Cho phép dùng PAID", userId, p.getId());
            return AccessCheckResponseDto.builder()
                    .allowed(true)
                    .type("PAID")
                    .activePaymentId(p.getId())
                    .message("Đã xác nhận thanh toán hợp lệ. Sẵn sàng tạo menu.")
                    .build();
        }

        // Trường hợp 3: Đã dùng miễn phí và chưa có thanh toán hợp lệ -> Bắt buộc thanh toán
        log.info("[ACCESS-CHECK] User ID={} đã dùng thử (free_used=true) và chưa thanh toán -> Yêu cầu thanh toán", userId);
        return AccessCheckResponseDto.builder()
                    .allowed(false)
                    .type("PAYMENT_REQUIRED")
                    .message("Bạn đã sử dụng hết lượt miễn phí. Vui lòng thanh toán để tiếp tục sử dụng dịch vụ.")
                    .build();
    }

    /**
     * Tạo giao dịch thanh toán kèm mã QR cố định của người dùng
     */
    @Transactional
    public PaymentCreateResponseDto createPayment(Long userId) {
        String transCode = "SM" + System.currentTimeMillis();

        Payment payment = Payment.builder()
                .userId(userId)
                .amount(DEFAULT_AMOUNT)
                .transactionCode(transCode)
                .status("PENDING")
                .consumed(false)
                .createdDate(LocalDateTime.now())
                .build();

        payment = paymentRepository.save(payment);
        log.info("[PAYMENT-CREATE] Tạo giao dịch Payment ID={} | Code={} | Amount={} | User={}",
                payment.getId(), transCode, DEFAULT_AMOUNT, userId);

        return PaymentCreateResponseDto.builder()
                .paymentId(payment.getId())
                .transactionCode(transCode)
                .amount(DEFAULT_AMOUNT)
                .qrImageUrl(DEFAULT_QR_IMAGE)
                .status("PENDING")
                .bankInfo("Vui lòng quét mã QR và nhập đúng nội dung chuyển khoản: " + transCode)
                .build();
    }

    /**
     * Polling kiểm tra trạng thái giao dịch
     */
    public Map<String, Object> checkStatus(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch: " + paymentId));

        return Map.of(
                "paymentId", payment.getId(),
                "transactionCode", payment.getTransactionCode(),
                "status", payment.getStatus(),
                "consumed", Boolean.TRUE.equals(payment.getConsumed()),
                "amount", payment.getAmount()
        );
    }

    public boolean belongsToUser(Long paymentId, Long userId) {
        return paymentRepository.findById(paymentId)
                .map(payment -> payment.getUserId().equals(userId))
                .orElse(false);
    }

    /**
     * Webhook Callback thanh toán từ Ngân hàng / Cổng thanh toán thật (SePay / Casso / Ngân hàng)
     */
    @Transactional
    public boolean handleWebhookCallback(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) return false;

        // Trích xuất chuỗi nội dung từ các trường thông dụng của ngân hàng
        String rawContent = "";
        if (payload.containsKey("content")) rawContent = String.valueOf(payload.get("content"));
        else if (payload.containsKey("description")) rawContent = String.valueOf(payload.get("description"));
        else if (payload.containsKey("transactionCode")) rawContent = String.valueOf(payload.get("transactionCode"));
        else if (payload.containsKey("orderCode")) rawContent = String.valueOf(payload.get("orderCode"));
        else if (payload.containsKey("code")) rawContent = String.valueOf(payload.get("code"));
        else rawContent = payload.toString();

        log.info("[PAYMENT-WEBHOOK] Nội dung giao dịch nhận được: {}", rawContent);

        // Regex tìm mã giao dịch dạng SM<timestamp>
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(SM\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(rawContent);

        String transCode = null;
        if (matcher.find()) {
            transCode = matcher.group(1);
        } else if (rawContent.contains("SM")) {
            transCode = rawContent.trim();
        }

        if (transCode == null) {
            log.warn("[PAYMENT-WEBHOOK-FAILED] Không tìm thấy mã giao dịch SM... trong nội dung: {}", rawContent);
            return false;
        }

        return handleWebhookCallback(transCode);
    }

    /**
     * Xử lý xác nhận giao dịch theo mã transactionCode
     */
    @Transactional
    public boolean handleWebhookCallback(String transactionCode) {
        if (transactionCode == null || transactionCode.isBlank()) return false;

        String cleanCode = transactionCode.trim();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(SM\\d+)").matcher(cleanCode);
        if (matcher.find()) {
            cleanCode = matcher.group(1);
        }

        Optional<Payment> opt = paymentRepository.findByTransactionCode(cleanCode);
        if (opt.isPresent()) {
            Payment p = opt.get();
            p.setStatus("SUCCESS");
            p.setPaidDate(LocalDateTime.now());
            paymentRepository.save(p);
            log.info("[PAYMENT-CALLBACK-SUCCESS] Xác nhận thanh toán tiền thật thành công cho mã Code: {} | User ID: {}", cleanCode, p.getUserId());
            return true;
        }

        log.warn("[PAYMENT-CALLBACK-FAILED] Không tìm thấy giao dịch với mã Code: {}", cleanCode);
        return false;
    }

    /**
     * Xác nhận thanh toán thành công (hỗ trợ kiểm thử demo ngay trên giao diện)
     */
    @Transactional
    public boolean mockConfirmPayment(Long paymentId) {
        Optional<Payment> opt = paymentRepository.findById(paymentId);
        if (opt.isPresent()) {
            Payment p = opt.get();
            p.setStatus("SUCCESS");
            p.setPaidDate(LocalDateTime.now());
            paymentRepository.save(p);
            log.info("[PAYMENT-MOCK-CONFIRM] Đã xác nhận thủ công thanh toán thành công cho Payment ID: {}", paymentId);
            return true;
        }
        return false;
    }

    /**
     * Ghi nhận lịch sử tạo menu và tiêu thụ quyền (Cập nhật free_used hoặc consumed = true)
     */
    @Transactional
    public void recordMenuGenerationAndConsume(Long userId, Long sessionId, String styleId, String menuImageUrl) {
        if (userId == null) {
            log.warn("[MENU-RECORD] Không có userId để ghi nhận lịch sử");
            return;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        // Nếu là lượt Free: cập nhật free_used = true
        if (user.getFreeUsed() == null || !user.getFreeUsed()) {
            user.setFreeUsed(true);
            userRepository.save(user);

            MenuGeneration record = MenuGeneration.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    .paymentId(null)
                    .generationType("FREE")
                    .styleId(styleId)
                    .menuImageUrl(menuImageUrl)
                    .createdDate(LocalDateTime.now())
                    .build();
            menuGenerationRepository.save(record);
            log.info("[MENU-RECORD-FREE] Đã ghi nhận lịch sử Free và cập nhật free_used=true cho User ID: {}", userId);
            return;
        }

        // Nếu là lượt Trả phí: tìm payment SUCCESS chưa consumed
        Optional<Payment> paidOpt = paymentRepository.findFirstByUserIdAndStatusAndConsumedFalseOrderByCreatedDateAsc(userId, "SUCCESS");
        Long paymentId = null;
        if (paidOpt.isPresent()) {
            Payment p = paidOpt.get();
            p.setConsumed(true);
            paymentRepository.save(p);
            paymentId = p.getId();
            log.info("[MENU-RECORD-PAID] Đã tiêu thụ Payment ID={} (consumed=true) cho User ID: {}", paymentId, userId);
        }

        MenuGeneration record = MenuGeneration.builder()
                .userId(userId)
                .sessionId(sessionId)
                .paymentId(paymentId)
                .generationType("PAID")
                .styleId(styleId)
                .menuImageUrl(menuImageUrl)
                .createdDate(LocalDateTime.now())
                .build();
        menuGenerationRepository.save(record);
        log.info("[MENU-RECORD-PAID] Đã ghi nhận lịch sử tạo menu PAID cho User ID: {}", userId);
    }
}
