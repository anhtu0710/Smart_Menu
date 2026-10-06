export interface AccessCheckResponse {
  allowed: boolean;
  type: 'FREE' | 'PAID' | 'PAYMENT_REQUIRED';
  message: string;
  activePaymentId?: number | null;
}

export interface BankInfo {
  bankName: string;
  accountNumber: string;
  accountHolder: string;
}

export interface PaymentCreateResponse {
  paymentId: number;
  transactionCode: string;
  amount: number;
  qrImageUrl: string;
  status: string;
  bankInfo: BankInfo;
}

export interface PaymentStatusResponse {
  paymentId: number;
  transactionCode: string;
  status: 'PENDING' | 'SUCCESS' | 'CANCELLED';
  consumed: boolean;
  paidDate?: string | null;
}
