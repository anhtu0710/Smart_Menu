export interface ContactRequest {
  fullName: string;
  email: string;
  phone?: string;
  subject?: string;
  message: string;
}

export interface ContactResponse {
  ticketId: string;
  status: string;
  receivedAt: string;
  message: string;
}
