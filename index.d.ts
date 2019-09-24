interface ObjectRef {
  id: string;
}

interface TipSuggestion {
  name: string;
  percentage: number;
}

interface Result {
  success: boolean;
}

interface Tender extends ObjectRef {
  label: string;
}

interface AuthenticationResult extends Result {
  errorMessage: string;
  authToken: string;
}

interface MerchantResult extends Result {
  statusMessage?: string;
  merchant: Merchant;
}

interface Merchant extends ObjectRef {
  name: string;
  email: string;
  location: MerchantLocation;
}

interface MerchantLocation {
  country: string;
  city: string;
  region: string;
}

interface TransactionResult extends Result {
  reason?: string;
  message?: string;
}

interface Transaction {
  id: string;
  amount: number;
  createdTime: string;
}

interface Payment extends Transaction {
  externalPaymentId: string;
  offline: boolean;
  tipAmount: number;
  order: ObjectRef;
  tender: Tender;
}

interface Refund extends Transaction {
  payment: ObjectRef;
}

interface Credit extends Transaction { 
  order: ObjectRef;
  tender: Tender;
}

interface CardEntryMethod {
  ICC_CONTACT: number;
  MAG_STRIPE: number;
  MANUAL: number;
  NFC_CONTACTLESS: number;
  VAULTED_CARD: number;
  ALL: number;
  /**
   * Custom method that matches Clover default of ICC_CONTACT, MAG_SWIPE, and NFC_CONTACTLESS
   */
  DEFAULT: number;
}

interface DataEntryLocation {
  NONE: string;
  ON_PAPER: string;
  ON_SCREEN: string;
}

interface VoidReason {
  AUTH_CLOSED_NEW_CARD: string;
  DEVELOPER_PAY_PARTIAL_AUTH: string;
  DEVELOPER_PAY_TIP_ADJUST_FAILED: string;
  FAILED: string;
  GIFTCARD_LOAD_FAILED: string;
  NOT_APPROVED: string; 
  REJECT_DUPLICATE: string; 
  REJECT_OFFLINE: string; 
  REJECT_PARTIAL_AUTH: string;
  REJECT_SIGNATURE: string;
  TRANSPORT_ERROR: string; 
  USER_CANCEL: string; 
  USER_CUSTOMER_CANCEL: string; 
  USER_GIFTCARD_LOAD_CANCEL: string; 
}

interface TipMode {
  NO_TIP: string;
  TIP_PROVIDED: string;
  ON_SCREEN_BEFORE_PAYMENT: string;
}

interface PrintJobFlag {
  FLAG_BILL: number;
  FLAG_CUSTOMER: number;
  FLAG_FORCE_SIGNATURE: number;
  FLAG_MERCHANT: number;
  FLAG_NO_SIGNATURE: number;
  FLAG_NONE: number;
  FLAG_REFUND: number;
  FLAG_REPRINT: number;
}

interface SaleOption {
  amount: number;
  externalPaymentId?: string;
  generateExternalPaymentId?: boolean;
  cardEntryMethods?: number;
  disableRestartTransactionOnFail?: boolean;
  disableDuplicateChecking?: boolean;
  disablePrinting?: boolean;
  disableReceiptSelection?: boolean;
  signatureEntryLocation?: string;
  signatureThreshold?: number;
  autoAcceptSignature?: boolean;
  tipAmount?: number;
  tippableAmount?: number;
  tipMode?: string;
  tipSuggestions?: Array<TipSuggestion>;
  printReceipt?: boolean;
}

interface SaleResult extends TransactionResult {
  payment: Payment;
}

interface RefundPaymentOption {
  paymentId: string;
  orderId: string;
  amount?: number;
  setFullRefund?: boolean;
  printReceipt?: boolean;
}

interface RefundPaymentResult extends TransactionResult {
  refund: Refund;
}

interface ManualRefundOption {
  amount: number;
  externalPaymentId?: string;
  generateExternalPaymentId?: boolean;
  cardEntryMethods?: number;
  disableRestartTransactionOnFail?: boolean;
  printReceipt?: boolean;
}

interface ManualRefundResult extends TransactionResult {
  credit: Credit;
}

interface VoidPaymentOption {
  paymentId: string;
  orderId: string;
  voidReason: string;
  printReceipt?: boolean;
}

interface VoidPaymentResult extends TransactionResult {
  paymentId: string;
}

interface VoidPaymentRefundOption {
  paymentId: string;
  orderId: string;
}

interface VoidPaymentRefundResult extends TransactionResult {
  paymentId: string;
  refundId: string;
}

interface PrintPaymentOption {
  orderId: string;
  paymentId: string;
  flags?: Array<number>;
}

/**
 * Another blah test
 */
declare const _default: {
  // General Methods ///////////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////////////////////////////
  /**
  * Obtains authentication data from the Clover service.
  * @param {boolean} [forceValidateToken = false] Flag to validate against API, increases latency, use only when needed.
  * @param {number} [timeout = 10000] Timeout in milliseconds.
  * @returns {Promise} A promise that resolves to an AuthenticationResult.
  */
  authenticate: (forceValidateToken?: boolean, timeout?: number) => Promise<AuthenticationResult>;
  /**
   * Obtains Merchant Info from the Clover service.
   * @returns {Promise} A promise that resolves to a MerchantResult.
   */
  getMerchant: () => Promise<MerchantResult>;
  enableCustomerMode: () => void;
  disableCustomerMode: () => void;
  print: (imagePath: string) => Promise<object>;
  printPayment: (option: PrintPaymentOption) => void;
  /**
   * Obtains required Android runtime permissions, only needed if targeting API > 25.
   * @returns {Promise<Result>} A promise that resolves to a Result.
   */
  startAccountChooserIfNeeded: () => Promise<Result>;

  // Payment Methods ///////////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////////////////////////////
  /**
   * Readies the Clover Bridge for sending payment requests to Clover device. Must be called before calling any payment method and should be 
   * called as soon as possible.
   * @param {string} raid Remote Application Id. Obtained from Clover App dashboard.
   */
  initializePaymentConnector: (raid: string) => void;
  sale: (option: SaleOption) => Promise<SaleResult>;
  refundPayment: (option: RefundPaymentOption) => Promise<RefundPaymentResult>;
  manualRefund: (option: ManualRefundOption) => Promise<ManualRefundResult>;
  voidPayment: (option: VoidPaymentOption) => Promise<VoidPaymentResult>;
  voidPaymentRefund: (option: VoidPaymentRefundOption) => Promise<VoidPaymentRefundResult>;
  /**
   * Forces the SPA to close
   */
  cancelSPA: () => void;

  // Constant Methods //////////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////////////////////////////
  isFlex: () => boolean;
  isMini: () => boolean;
  getSpaVersion: () => string;

  // Enums/Constants ///////////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////////////////////////////
  /**
   * Clover HSN
   */
  HARDWARE_SERIAL_NUMBER: String;
  CARD_ENTRY_METHOD: CardEntryMethod;
  /**
   * https://clover.github.io/clover-android-sdk/com/clover/sdk/v3/payments/DataEntryLocation.html
   */
  DATA_ENTRY_LOCATION: DataEntryLocation;
  /**
   * https://clover.github.io/clover-android-sdk/com/clover/sdk/v3/order/VoidReason.html
   */
  VOID_REASON: VoidReason;
  /**
   * https://clover.github.io/clover-android-sdk/com/clover/sdk/v3/payments/TipMode.html
   */
  TIP_MODE: TipMode;
  /**
   * https://clover.github.io/clover-android-sdk/com/clover/sdk/v1/printer/job/PrintJob.html
   */
  PRINT_JOB_FLAG: PrintJobFlag;
}

export default _default;