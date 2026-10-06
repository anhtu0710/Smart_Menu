import { Component, inject, signal, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AiMenuService } from '../../core/services/ai-menu.service';
import { PaymentService } from '../../core/services/payment.service';
import { PaymentCreateResponse, AccessCheckResponse } from '../../core/models/payment.model';
import {
  FullAnalysisResponseData,
  ProductItemDto,
  AnalysisReportDto,
  FnbRuleDto,
  RecommendationDto,
  CategoryRevenueDto,
  MenuRestructurePlanDto,
  PriorityProductDto,
  VisualMenuBlueprint,
  MenuStyleOption,
  BlueprintSectionDto
} from '../../core/models/ai-menu.model';
import { VisualMenuRendererComponent } from './components/visual-menu-renderer/visual-menu-renderer.component';
import html2canvas from 'html2canvas';
import { jsPDF } from 'jspdf';

@Component({
  selector: 'app-dich-vu',
  standalone: true,
  imports: [CommonModule, FormsModule, VisualMenuRendererComponent],
  templateUrl: './dichvu.component.html',
  styleUrls: ['./dichvu.component.scss']
})
export class DichVuComponent implements OnInit, OnDestroy {
  private aiMenuService = inject(AiMenuService);
  private paymentService = inject(PaymentService);

  // Tiến trình 4 bước chính trên Header (1: Tải dữ liệu, 2: AI phân tích, 3: Tạo menu tối ưu, 4: Xuất hình ảnh)
  currentStep = signal<number>(1);

  // Quản lý thông tin Session & File Upload thực tế
  sessionId = signal<string>('');
  
  businessFile = signal<File | null>(null);
  businessFileName = signal<string | null>(null);
  businessFileSize = signal<string | null>(null);

  oldMenuFile = signal<File | null>(null);
  oldMenuName = signal<string | null>(null);
  oldMenuPreviewUrl = signal<string | null>(null);

  // Trạng thái Drag & Drop
  isOldMenuHover = signal<boolean>(false);
  isBizFileHover = signal<boolean>(false);

  // Trạng thái Loading & Dữ liệu đã phân tích
  isProcessing = signal<boolean>(false);
  hasAnalyzedData = signal<boolean>(false);

  // Dữ liệu chỉ số tổng quan (Metrics Stat Grid)
  totalRevenueStr = signal<string>('—');
  totalSalesStr = signal<string>('—');
  estimatedProfitStr = signal<string>('—');
  menuOptimizationScoreStr = signal<string>('—');

  // Dữ liệu 3 nhóm món ăn đặc biệt
  bestSellersText = signal<string>('—');
  highProfitText = signal<string>('—');
  lowPriorityText = signal<string>('—');

  // Dữ liệu phân tích sâu từ Backend / Gemini AI
  analysisReport = signal<AnalysisReportDto | null>(null);
  productsList = signal<ProductItemDto[]>([]);
  fnbRulesList = signal<FnbRuleDto[]>([]);
  geminiReasoningText = signal<string>('');
  categoryRevenueList = signal<CategoryRevenueDto[]>([]);

  // Dữ liệu Restructure Plan từ AI
  restructurePlan = signal<MenuRestructurePlanDto | null>(null);

  // Dữ liệu VisualMenuBlueprint & AI Style Selection
  menuBlueprint = signal<VisualMenuBlueprint | null>(null);
  selectedStyleId = signal<string>('HIEN_DAI');

  coffeeTemplatesList = [
    { templateId: 'HIEN_DAI', templateName: 'Phong cách Hiện Đại', description: 'Nền Slate hiện đại, phân cấp thị giác rõ nét, typography thương mại (Bản Free)', badgeText: 'HIỆN ĐẠI', isPlusOnly: false, primaryColor: '#38bdf8' },
    { templateId: 'SANG_TRONG', templateName: 'Phong cách Sang Trọng', description: 'Tông đen huyền bí kết hợp vàng đồng sang trọng, cao cấp (Gói Plus)', badgeText: 'SANG TRỌNG', isPlusOnly: true, primaryColor: '#fbbf24' },
    { templateId: 'TRUYEN_THONG', templateName: 'Phong cách Truyền Thống', description: 'Gam màu ấm áp cổ điển, phong vị mộc mạc gần gũi (Gói Plus)', badgeText: 'TRUYỀN THỐNG', isPlusOnly: true, primaryColor: '#d97706' },
    { templateId: 'TRE_TRUNG', templateName: 'Phong cách Trẻ Trung', description: 'Tươi vui, năng động, tương phản sắc màu bắt mắt (Gói Plus)', badgeText: 'TRẺ TRUNG', isPlusOnly: true, primaryColor: '#db2777' }
  ];

  // Trạng thái Modal Zoom Fullsize Xem Menu
  isModalOpen = signal<boolean>(false);
  modalTitle = signal<string>('');
  modalType = signal<'OLD_MENU' | 'NEW_MENU'>('OLD_MENU');

  // Trạng thái Định dạng xuất file (png | jpg | pdf)
  selectedExportFormat = signal<'png' | 'jpg' | 'pdf'>('png');

  // Trạng thái Toast thông báo
  showToast = signal<boolean>(false);
  toastMessage = signal<string>('');

  // Trạng thái Quyền người dùng & Phong cách chờ kích hoạt (Free vs Plus)
  userAccessType = signal<'FREE' | 'PAID' | 'PAYMENT_REQUIRED'>('FREE');
  pendingStyleId = signal<string | null>(null);

  // Trạng thái Quản lý Thanh toán & Lượt sử dụng
  isPaymentModalOpen = signal<boolean>(false);
  paymentModalTitle = signal<string>('Thanh toán dịch vụ Thiết Kế Menu');
  paymentModalSubtitle = signal<string>('Mở khóa phân tích doanh thu & xuất thực đơn AI chuyên nghiệp');
  paymentNoticeText = signal<string>('Bạn đã hoàn thành lượt trải nghiệm miễn phí. Phí dịch vụ: 50.000 VNĐ / lần thiết kế.');
  paymentData = signal<PaymentCreateResponse | null>(null);
  paymentStatus = signal<'PENDING' | 'SUCCESS' | 'CANCELLED'>('PENDING');
  isCheckingPayment = signal<boolean>(false);
  isCreatingPayment = signal<boolean>(false);
  codeCopied = signal<boolean>(false);
  readonly qrVersion = Date.now();
  private pollingTimer: any = null;

  ngOnInit() {
    this.checkUserAccessStatus();
  }

  checkUserAccessStatus() {
    this.paymentService.checkAccess().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.userAccessType.set(res.data.type);
        }
      },
      error: () => {
        this.userAccessType.set('FREE');
      }
    });
  }

  ngOnDestroy() {
    this.stopPaymentPolling();
  }

  // Chuyển đổi giữa các bước tiến trình 1..4 trên header
  goToStep(step: number) {
    if (step > 1 && !this.hasAnalyzedData() && !this.isProcessing()) {
      this.triggerToast('⚠️ Vui lòng tải đủ file và bấm "Bắt đầu phân tích" trước!');
      return;
    }
    this.currentStep.set(step);
  }

  // 1. Xử lý nạp File Menu cũ (PDF, JPG, PNG)
  onOldMenuSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      this.handleOldMenuFile(file);
    }
  }

  handleOldMenuFile(file: File) {
    this.oldMenuFile.set(file);
    this.oldMenuName.set(file.name);
    
    // Tạo preview image nếu là file ảnh
    if (file.type.startsWith('image/')) {
      const reader = new FileReader();
      reader.onload = (e: any) => {
        this.oldMenuPreviewUrl.set(e.target.result);
      };
      reader.readAsDataURL(file);
    } else {
      this.oldMenuPreviewUrl.set(null);
    }
    this.triggerToast(`✨ Đã nạp file menu cũ: ${file.name}`);
  }

  removeOldMenu() {
    this.oldMenuFile.set(null);
    this.oldMenuName.set(null);
    this.oldMenuPreviewUrl.set(null);
  }

  // 2. Xử lý nạp File Dữ liệu kinh doanh (Excel, CSV)
  onBusinessFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      this.handleBusinessFile(file);
    }
  }

  handleBusinessFile(file: File) {
    this.businessFile.set(file);
    this.businessFileName.set(file.name);
    const sizeKB = (file.size / 1024).toFixed(1);
    this.businessFileSize.set(`${sizeKB} KB`);
    this.triggerToast(`📊 Đã nạp file dữ liệu kinh doanh: ${file.name}`);
  }

  removeBusinessFile() {
    this.businessFile.set(null);
    this.businessFileName.set(null);
    this.businessFileSize.set(null);
  }

  // Drag and Drop Events
  onDragOver(event: DragEvent, zone: 'menu' | 'biz') {
    event.preventDefault();
    event.stopPropagation();
    if (zone === 'menu') this.isOldMenuHover.set(true);
    if (zone === 'biz') this.isBizFileHover.set(true);
  }

  onDragLeave(event: DragEvent, zone: 'menu' | 'biz') {
    event.preventDefault();
    event.stopPropagation();
    if (zone === 'menu') this.isOldMenuHover.set(false);
    if (zone === 'biz') this.isBizFileHover.set(false);
  }

  onDropFile(event: DragEvent, zone: 'menu' | 'biz') {
    event.preventDefault();
    event.stopPropagation();
    if (zone === 'menu') this.isOldMenuHover.set(false);
    if (zone === 'biz') this.isBizFileHover.set(false);

    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      if (zone === 'menu') this.handleOldMenuFile(files[0]);
      if (zone === 'biz') this.handleBusinessFile(files[0]);
    }
  }

  // Kích hoạt Quy trình Bắt đầu: Kiểm tra điều kiện file & Quyền sử dụng (Free / Payment)
  // Kích hoạt Quy trình Bắt đầu: Kiểm tra điều kiện file & Quyền sử dụng (Free / Payment)
  onStartAnalysis() {
    const excel = this.businessFile();
    const oldMenu = this.oldMenuFile();

    if (!excel || !oldMenu) {
      this.triggerToast('⚠️ Vui lòng tải lên cả File dữ liệu kinh doanh (Excel) và Ảnh menu cũ trước khi phân tích!');
      return;
    }

    this.isProcessing.set(true);
    this.triggerToast('🔍 Đang kiểm tra quyền sử dụng dịch vụ...');

    // Bước 0: Kiểm tra quyền từ Backend (Free / Paid / Bắt buộc thanh toán)
    this.paymentService.checkAccess().subscribe({
      next: (accessRes) => {
        this.isProcessing.set(false);
        if (!accessRes.success || !accessRes.data) {
          this.triggerToast(`❌ Lỗi kiểm tra quyền: ${accessRes.message || 'Không có phản hồi từ máy chủ'}`);
          return;
        }

        const access = accessRes.data;
        this.userAccessType.set(access.type);
        if (access.type === 'FREE' && access.allowed) {
          this.triggerToast(`🎁 ${access.message || 'Bạn được sử dụng 01 lượt thiết kế menu miễn phí!'}`);
          this.executeUploadAndAnalyze();
        } else if (access.type === 'PAID' && access.allowed) {
          this.triggerToast(`✅ ${access.message || 'Lượt thiết kế menu này đã được thanh toán!'}`);
          this.executeUploadAndAnalyze();
        } else {
          // Bất kể là PAYMENT_REQUIRED hay chưa thanh toán: Bật ngay modal thanh toán QR!
          this.paymentModalTitle.set('Thanh toán dịch vụ Thiết Kế Menu');
          this.paymentModalSubtitle.set('Mở khóa phân tích doanh thu & xuất thực đơn AI chuyên nghiệp');
          this.paymentNoticeText.set('Bạn đã hoàn thành lượt trải nghiệm miễn phí. Phí dịch vụ: 50.000 VNĐ / lần thiết kế.');
          this.openPaymentModal();
        }
      },
      error: (err) => {
        this.isProcessing.set(false);
        const errDetail = err.error?.message || err.message || '';
        if (errDetail.includes('PAYMENT_REQUIRED') || err.status === 402) {
          this.openPaymentModal();
        } else {
          this.triggerToast(`❌ Lỗi kiểm tra quyền: ${errDetail || 'Không thể kết nối đến máy chủ xác thực'}`);
        }
      }
    });
  }

  // Mở Popup Thanh toán QR & Khởi tạo giao dịch
  openPaymentModal() {
    this.isPaymentModalOpen.set(true);
    this.isCreatingPayment.set(true);
    this.paymentStatus.set('PENDING');

    this.paymentService.createPayment().subscribe({
      next: (res) => {
        this.isCreatingPayment.set(false);
        if (res.success && res.data) {
          this.paymentData.set(res.data);
          this.startPaymentPolling(res.data.paymentId);
        } else {
          this.triggerToast(`❌ Lỗi tạo giao dịch: ${res.message || 'Thử lại sau'}`);
        }
      },
      error: (err) => {
        this.isCreatingPayment.set(false);
        const errMsg = err.error?.message || err.message || 'Không thể tạo mã thanh toán';
        this.triggerToast(`❌ Lỗi: ${errMsg}`);
      }
    });
  }

  // Đóng Modal Thanh toán
  closePaymentModal() {
    this.stopPaymentPolling();
    this.isPaymentModalOpen.set(false);
  }

  // Bắt đầu polling kiểm tra trạng thái thanh toán mỗi 2.5 giây
  startPaymentPolling(paymentId: number) {
    this.stopPaymentPolling();
    this.pollingTimer = setInterval(() => {
      this.paymentService.checkPaymentStatus(paymentId).subscribe({
        next: (res) => {
          if (res.success && res.data && res.data.status === 'SUCCESS') {
            this.handlePaymentSuccess();
          }
        },
        error: () => {
          // Bỏ qua lỗi mạng chập chờn khi polling
        }
      });
    }, 2500);
  }

  // Dừng polling
  stopPaymentPolling() {
    if (this.pollingTimer) {
      clearInterval(this.pollingTimer);
      this.pollingTimer = null;
    }
  }

  // Xử lý khi thanh toán thành công
  handlePaymentSuccess() {
    this.stopPaymentPolling();
    this.paymentStatus.set('SUCCESS');
    this.userAccessType.set('PAID');
    this.triggerToast('🎉 Thanh toán gói Plus thành công! Hệ thống đang kích hoạt dịch vụ...');
    setTimeout(() => {
      this.isPaymentModalOpen.set(false);
      const pending = this.pendingStyleId();
      if (pending) {
        this.pendingStyleId.set(null);
        this.triggerToast(`✨ Gói Plus đã kích hoạt! Đang chuyển sang phong cách "${pending}"...`);
        this.onSelectStyle(pending);
      } else if (!this.hasAnalyzedData()) {
        this.executeUploadAndAnalyze();
      }
    }, 1500);
  }

  // Nút kiểm tra trạng thái chủ động
  verifyPaymentManually() {
    const data = this.paymentData();
    if (!data) return;
    this.isCheckingPayment.set(true);
    this.paymentService.checkPaymentStatus(data.paymentId).subscribe({
      next: (res) => {
        this.isCheckingPayment.set(false);
        if (res.success && res.data?.status === 'SUCCESS') {
          this.handlePaymentSuccess();
        } else {
          this.triggerToast('⏳ Hệ thống chưa ghi nhận tiền về tài khoản. Vui lòng đợi trong giây lát hoặc kiểm tra đúng nội dung chuyển khoản.');
        }
      },
      error: (err) => {
        this.isCheckingPayment.set(false);
        this.triggerToast('❌ Lỗi kiểm tra thanh toán: ' + (err.error?.message || err.message));
      }
    });
  }


  // Sao chép mã chuyển khoản
  copyTransactionCode() {
    const code = this.paymentData()?.transactionCode;
    if (code) {
      navigator.clipboard.writeText(code).then(() => {
        this.codeCopied.set(true);
        this.triggerToast(`📋 Đã sao chép mã chuyển khoản: ${code}`);
        setTimeout(() => this.codeCopied.set(false), 2500);
      });
    }
  }

  // Tiến hành tải file và kích hoạt chu trình phân tích
  private executeUploadAndAnalyze() {
    const excel = this.businessFile();
    const oldMenu = this.oldMenuFile();

    if (!excel || !oldMenu) return;

    this.isProcessing.set(true);
    this.hasAnalyzedData.set(false);
    this.triggerToast('⏳ Đang tải file dữ liệu lên hệ thống Backend...');

    console.log('Start analysis', { excel, oldMenu });

    // Bước 1: Upload file lên Spring Boot Backend REST API (/api/v1/upload/menu-analysis)
    this.aiMenuService.uploadMenuAnalysis(excel, oldMenu).subscribe({
      next: (uploadRes) => {
        console.log('Upload response:', uploadRes);
        if (!uploadRes.success || !uploadRes.data?.sessionId) {
          this.isProcessing.set(false);
          this.triggerToast(`❌ Lỗi upload file: ${uploadRes.message || 'Không thể tạo phiên phân tích'}`);
          return;
        }

        const backendSessionId = uploadRes.data.sessionId;
        this.sessionId.set(String(backendSessionId));
        console.log('Session ID:', backendSessionId);
        this.triggerToast('🚀 AI đang tiến hành đọc dữ liệu Excel & phân tích Gemini...');

        // Bước 2: Kích hoạt toàn bộ workflow phân tích từ Spring Boot Backend (/api/v1/smartcoffee/analyze-full/{sessionId})
        this.aiMenuService.analyzeFullWorkflow(backendSessionId).subscribe({
          next: (analysisRes) => {
            console.log('Analysis response:', analysisRes);
            this.isProcessing.set(false);
            if (analysisRes.success && analysisRes.data) {
              this.applyAnalysisResult(analysisRes.data);
            } else {
              this.triggerToast(`❌ Phân tích thất bại: ${analysisRes.message || 'Không nhận được dữ liệu'}`);
            }
          },
          error: (err) => {
            this.isProcessing.set(false);
            const errDetail = err.error?.message || err.message || '';
            if (errDetail.includes('PAYMENT_REQUIRED') || err.status === 402) {
              this.triggerToast('💳 Bạn đã sử dụng hết lượt miễn phí. Vui lòng thanh toán để tiếp tục sử dụng dịch vụ.');
              this.openPaymentModal();
              return;
            }
            this.triggerToast(`❌ Lỗi khi phân tích AI: ${errDetail || 'Lỗi xử lý AI Backend'}`);
          }
        });
      },
      error: (err) => {
        this.isProcessing.set(false);
        const errDetail = err.error?.message || err.message || '';
        if (errDetail.includes('PAYMENT_REQUIRED') || err.status === 402) {
          this.triggerToast('💳 Bạn đã sử dụng hết lượt miễn phí. Vui lòng thanh toán để tiếp tục sử dụng dịch vụ.');
          this.openPaymentModal();
          return;
        }
        this.triggerToast(`❌ Lỗi upload file: ${errDetail || 'Không thể kết nối đến Backend Spring Boot'}`);
      }
    });
  }

  private applyAnalysisResult(data: FullAnalysisResponseData) {
    this.hasAnalyzedData.set(true);
    this.currentStep.set(2); // Chuyển sang Bước 2: AI phân tích

    this.analysisReport.set(data.analysis || null);
    this.productsList.set(data.products || []);
    this.fnbRulesList.set(data.fnbRules || []);
    this.categoryRevenueList.set(data.categoryRevenue || []);
    this.geminiReasoningText.set(data.reasoning || 'AI đã phân tích xong cấu trúc thực đơn và dữ liệu kinh doanh của bạn.');
    this.restructurePlan.set(data.restructurePlan || null);

    if (data.analysis) {
      this.totalRevenueStr.set(data.analysis.totalRevenue ? `${data.analysis.totalRevenue.toLocaleString('vi-VN')}đ` : '0đ');
      this.totalSalesStr.set(data.analysis.totalItemsSold ? `${data.analysis.totalItemsSold.toLocaleString('vi-VN')} phần` : '0 phần');
      this.estimatedProfitStr.set(data.analysis.estimatedProfit ? `${data.analysis.estimatedProfit.toLocaleString('vi-VN')}đ` : (data.analysis.averageMargin ? `${data.analysis.averageMargin}%` : '0%'));
      this.menuOptimizationScoreStr.set(data.analysis.optimizationScore ? `${data.analysis.optimizationScore}/100` : '0/100');

      this.bestSellersText.set(data.analysis.bestSellersText || this.extractTopDishes(data.products, 'sales'));
      this.highProfitText.set(data.analysis.highProfitText || this.extractTopDishes(data.products, 'profit'));
      this.lowPriorityText.set(data.analysis.lowPriorityText || this.extractTopDishes(data.products, 'dog'));
    } else {
      this.populateAiInsightsFromProducts(data.products || []);
    }

    // Tải hoặc Tạo VisualMenuBlueprint từ Backend
    this.loadOrCreateBlueprint(data);

    this.triggerToast('✨ Phân tích SmartMenu thành công dữ liệu thực tế từ Spring Boot Backend!');
  }

  private loadOrCreateBlueprint(data: FullAnalysisResponseData) {
    if (!data.sessionId) return;

    this.aiMenuService.getMenuBlueprint(data.sessionId).subscribe({
      next: (res) => {
        if (res.success && res.data && res.data.blueprint) {
          this.menuBlueprint.set(res.data.blueprint);
          if (res.data.selectedStyleId) {
            this.selectedStyleId.set(res.data.selectedStyleId as any);
          }
        } else {
          this.buildLocalBlueprint(data.products, data.restructurePlan);
        }
      },
      error: () => {
        this.buildLocalBlueprint(data.products, data.restructurePlan);
      }
    });
  }

  private buildLocalBlueprint(products: ProductItemDto[] | undefined, plan: MenuRestructurePlanDto | null | undefined) {
    if (!products || products.length === 0) return;

    const findMatches = (names: string[] | undefined) => {
      if (!names) return [];
      return products.filter(p => names.some(n => p.name && n && p.name.toLowerCase().includes(n.toLowerCase().trim())));
    };

    const heroDishes = findMatches(plan?.heroDishes);
    const coreDishes = findMatches(plan?.coreDishes);
    const reducedPriorityItems = findMatches(plan?.reducedPriorityItems);
    const imagePriority = findMatches(plan?.imagePriority);

    // Group by category
    const catMap = new Map<string, ProductItemDto[]>();
    for (const p of products) {
      const cat = p.category && p.category.trim() ? p.category.trim() : 'Danh mục khác';
      if (!catMap.has(cat)) catMap.set(cat, []);
      catMap.get(cat)!.push(p);
    }

    const categoryOrder = plan?.categoryOrder && plan.categoryOrder.length > 0
      ? plan.categoryOrder
      : Array.from(catMap.keys());

    const sections: BlueprintSectionDto[] = [];
    for (const catName of categoryOrder) {
      const dishes = catMap.get(catName);
      if (dishes && dishes.length > 0) {
        sections.push({
          categoryName: catName,
          priority: 'NORMAL',
          dishes
        });
      }
    }

    // Add remaining categories
    for (const [catName, dishes] of catMap.entries()) {
      if (!sections.some(s => s.categoryName === catName)) {
        sections.push({
          categoryName: catName,
          priority: 'NORMAL',
          dishes
        });
      }
    }

    const blueprint: VisualMenuBlueprint = {
      version: 1,
      heroDishes,
      coreDishes,
      categoryOrder,
      reducedPriorityItems,
      comboPlacements: plan?.comboPlacements || [],
      structuredCombos: plan?.structuredCombos || [],
      priceRecommendations: plan?.priceRecommendations || [],
      imagePriority,
      sections,
      layoutSummary: plan?.finalLayoutSummary || 'Bố cục menu được sắp xếp theo mức độ ưu tiên kinh doanh.'
    };

    this.menuBlueprint.set(blueprint);
  }

  // Đổi Coffee Template (CHỈ ĐỔI TEMPLATE RENDERING - KHÔNG GỌI LẠI AI / KHÔNG THAY ĐỔI BLUEPRINT)
  onSelectStyle(styleId: string) {
    // Nghiệp vụ: Bản Free chỉ được zen style Hiện Đại, các style còn lại hiện cửa sổ thanh toán với thông báo mua gói Plus
    if (styleId !== 'HIEN_DAI' && this.userAccessType() !== 'PAID') {
      this.pendingStyleId.set(styleId);
      const styleItem = this.coffeeTemplatesList.find(t => t.templateId === styleId);
      const styleDisplayName = styleItem ? styleItem.templateName : styleId;

      this.paymentModalTitle.set('👑 NÂNG CẤP GÓI PLUS - TRẢI NGHIỆM ĐA PHONG CÁCH');
      this.paymentModalSubtitle.set('Bản Free chỉ được trải nghiệm style Hiện Đại. Mua gói Plus để mở khóa toàn bộ phong cách cao cấp!');
      this.paymentNoticeText.set(`Bạn đang chọn "${styleDisplayName}". Phong cách này chỉ dành cho gói Plus. Vui lòng nâng cấp gói để được trải nghiệm!`);
      this.triggerToast(`👑 Bản Free chỉ được dùng style Hiện Đại. Vui lòng mua gói Plus để trải nghiệm "${styleDisplayName}"!`);
      
      this.openPaymentModal();
      return;
    }

    this.selectedStyleId.set(styleId);

    const activeSession = this.sessionId();
    if (activeSession) {
      this.aiMenuService.updateMenuStyle(activeSession, styleId).subscribe({
        next: () => {
          this.triggerToast(`✨ Đã đổi Mẫu Menu Coffee sang "${styleId}" (VisualMenuBlueprint giữ nguyên 100%)`);
        },
        error: (err) => {
          console.warn('Lỗi lưu style ở backend:', err);
          this.triggerToast(`✨ Đã đổi Mẫu Menu Coffee sang "${styleId}"`);
        }
      });
    } else {
      this.triggerToast(`✨ Đã đổi Mẫu Menu Coffee sang "${styleId}"`);
    }
  }

  // Mở Popup Modal Phóng to Zoom Fullsize Xem Menu
  openOldMenuModal() {
    if (!this.oldMenuPreviewUrl()) {
      this.triggerToast('⚠️ Chưa có ảnh menu cũ để phóng to');
      return;
    }
    this.modalType.set('OLD_MENU');
    this.modalTitle.set('XEM NGUYÊN BẢN MENU CŨ (FULL CONTENT)');
    this.isModalOpen.set(true);
  }

  openNewMenuModal() {
    if (!this.hasAnalyzedData()) {
      this.triggerToast('⚠️ Chưa có dữ liệu menu mới để phóng to');
      return;
    }
    this.modalType.set('NEW_MENU');
    this.modalTitle.set(`BẢN THIẾT KẾ MENU MỚI (VISUAL MENU BLUEPRINT • STYLE ${this.selectedStyleId()})`);
    this.isModalOpen.set(true);
  }

  closeModal() {
    this.isModalOpen.set(false);
  }

  private extractTopDishes(products: ProductItemDto[] | undefined, type: 'sales' | 'profit' | 'dog'): string {
    if (!products || products.length === 0) return 'Chưa có dữ liệu';
    if (type === 'sales') {
      return [...products].sort((a, b) => b.salesQuantity - a.salesQuantity).slice(0, 3).map(p => p.name).join(', ');
    }
    if (type === 'profit') {
      return [...products].sort((a, b) => (b.profitMargin || 0) - (a.profitMargin || 0)).slice(0, 3).map(p => `${p.name} (${Math.round(p.profitMargin || 0)}% lãi)`).join(', ');
    }
    if (type === 'dog') {
      const dogs = products.filter(p => p.bcgCategory === 'DOG');
      const list = dogs.length > 0 ? dogs : products.slice(-2);
      return list.map(p => p.name).join(', ');
    }
    return 'Chưa có dữ liệu';
  }

  private populateAiInsightsFromProducts(products: ProductItemDto[]) {
    if (!products || products.length === 0) return;
    this.bestSellersText.set(this.extractTopDishes(products, 'sales'));
    this.highProfitText.set(this.extractTopDishes(products, 'profit'));
    this.lowPriorityText.set(this.extractTopDishes(products, 'dog'));
  }

  // Chọn định dạng xuất file (PNG | JPG | PDF)
  selectExportFormat(format: 'png' | 'jpg' | 'pdf') {
    this.selectedExportFormat.set(format);
  }

  // Thực hiện xuất file hình ảnh / PDF trực tiếp từ Rendered Menu DOM Element (Shared Renderer)
  exportMenuFile() {
    if (!this.hasAnalyzedData()) {
      this.triggerToast('⚠️ Cần có kết quả phân tích để xuất file menu tối ưu!');
      return;
    }

    const fmt = this.selectedExportFormat();
    this.currentStep.set(4);
    this.triggerToast(`⏳ Đang chụp & xuất file ${fmt.toUpperCase()} từ Rendered Menu...`);

    const element = document.getElementById('renderedMenuContainer');
    if (!element) {
      this.triggerToast('❌ Không tìm thấy giao diện Menu mới để xuất');
      return;
    }

    html2canvas(element, {
      scale: 2,
      useCORS: true,
      backgroundColor: null
    }).then(canvas => {
      const fileName = `SmartMenu_${this.selectedStyleId()}_${Date.now()}`;

      if (fmt === 'png' || fmt === 'jpg') {
        const imageType = fmt === 'png' ? 'image/png' : 'image/jpeg';
        const imgData = canvas.toDataURL(imageType, 0.95);
        const a = document.createElement('a');
        a.href = imgData;
        a.download = `${fileName}.${fmt}`;
        a.click();
        this.triggerToast(`🎉 Tải xuống thành công file ${fmt.toUpperCase()} bản thiết kế menu!`);
      } else if (fmt === 'pdf') {
        const imgData = canvas.toDataURL('image/png');
        const pdf = new jsPDF({
          orientation: 'portrait',
          unit: 'px',
          format: [canvas.width, canvas.height]
        });
        pdf.addImage(imgData, 'PNG', 0, 0, canvas.width, canvas.height);
        pdf.save(`${fileName}.pdf`);
        this.triggerToast('🎉 Tải xuống thành công file PDF bản in ấn thực đơn!');
      }
    }).catch(err => {
      console.error('Lỗi canvas export:', err);
      this.triggerToast('❌ Có lỗi khi tạo file hình ảnh từ Rendered Menu');
    });
  }

  // UI Presentation Helpers cho Restructure Plan Dashboard
  getProductName(product: any): string {
    if (!product) return '';
    if (typeof product === 'string') return product;
    return product.name || product.productName || String(product);
  }

  getProductImage(product: any): string | null {
    if (!product) return null;
    if (typeof product === 'object' && product.imageUrl) return product.imageUrl;
    return null;
  }

  getLayoutTopZone(): string {
    const plan = this.restructurePlan();
    if (plan?.topZone) return plan.topZone;
    return 'Vùng Tam Giác Vàng (Top-Right): Đặt ảnh minh họa lớn cho các món Hero Dish đặc sắc.';
  }

  getLayoutCenterZone(): string {
    const plan = this.restructurePlan();
    if (plan?.centerZone) return plan.centerZone;
    return 'Vùng trung tâm: Sắp xếp nổi bật các danh mục món ăn đóng góp tỷ trọng doanh thu lớn nhất.';
  }

  getLayoutSecondaryZone(): string {
    const plan = this.restructurePlan();
    if (plan?.secondaryZone) return plan.secondaryZone;
    return 'Khu vực phụ: Thu nhỏ font chữ, giản lược ảnh minh họa với các món bán chậm.';
  }

  // State toggle "Xem thêm" cho Món cần giảm ưu tiên
  showAllLowPriority = signal<boolean>(false);

  toggleLowPriorityView() {
    this.showAllLowPriority.update(v => !v);
  }

  // Top món bán chạy dạng danh sách có cấu trúc
  getTopSellersStructured(): Array<{ rank: number; name: string; quantity: number; revenue: number }> {
    const products = this.productsList();
    if (!products || products.length === 0) return [];

    return [...products]
      .filter(p => p.salesQuantity > 0)
      .sort((a, b) => b.salesQuantity - a.salesQuantity)
      .slice(0, 3)
      .map((p, idx) => ({
        rank: idx + 1,
        name: p.name,
        quantity: p.salesQuantity,
        revenue: p.totalRevenue || (p.salesQuantity * p.originalPrice)
      }));
  }

  // Top món lợi nhuận cao dạng danh sách có cấu trúc
  getHighProfitStructured(): Array<{ rank: number; name: string; margin: number }> {
    const products = this.productsList();
    if (!products || products.length === 0) return [];

    return [...products]
      .filter(p => (p.profitMargin || 0) > 0)
      .sort((a, b) => (b.profitMargin || 0) - (a.profitMargin || 0))
      .slice(0, 3)
      .map((p, idx) => ({
        rank: idx + 1,
        name: p.name,
        margin: Math.round((p.profitMargin || 0) * 10) / 10
      }));
  }

  // Món cần giảm ưu tiên dạng danh sách sản phẩm thật đã qua Data Sanitization
  getLowPriorityStructured(): Array<{ name: string; category: string; quantity: number }> {
    const plan = this.restructurePlan();
    const products = this.productsList();
    if (!products || products.length === 0) return [];

    let items: ProductItemDto[] = [];
    if (plan?.reducedPriorityItems && plan.reducedPriorityItems.length > 0) {
      items = products.filter(p => plan.reducedPriorityItems!.some(name => p.name.toLowerCase().includes(name.toLowerCase())));
    }

    if (items.length === 0) {
      const dogs = products.filter(p => p.bcgCategory === 'DOG');
      items = dogs.length > 0 ? dogs : [...products].sort((a, b) => a.salesQuantity - b.salesQuantity).slice(0, 5);
    }

    // Deduplicate
    const uniqueMap = new Map<string, ProductItemDto>();
    for (const p of items) {
      if (!uniqueMap.has(p.name.toLowerCase().trim())) {
        uniqueMap.set(p.name.toLowerCase().trim(), p);
      }
    }

    return Array.from(uniqueMap.values()).map(p => ({
      name: p.name,
      category: p.category || 'Danh mục',
      quantity: p.salesQuantity
    }));
  }

  // Sản phẩm ưu tiên cụ thể (Priority Products DTO) từ Session thực tế
  getPriorityProductsList(): PriorityProductDto[] {
    const plan = this.restructurePlan();
    if (plan?.priorityProducts && plan.priorityProducts.length > 0) {
      return plan.priorityProducts;
    }

    const products = this.productsList();
    if (!products || products.length === 0) return [];

    const topSelling = [...products].sort((a, b) => b.salesQuantity - a.salesQuantity).slice(0, 3);
    return topSelling.map(p => ({
      productId: p.id || String(Math.random()),
      productName: p.name,
      category: p.category || 'Danh mục',
      priorityType: p.bcgCategory === 'STAR' ? 'HERO' : 'CORE',
      reason: `Sản lượng bán cao nhất (${p.salesQuantity} phần) trong phiên phân tích hiện tại`
    }));
  }

  private triggerToast(message: string) {
    this.toastMessage.set(message);
    this.showToast.set(true);
    setTimeout(() => {
      this.showToast.set(false);
    }, 4000);
  }
}
