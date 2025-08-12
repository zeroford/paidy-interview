package forex.domain.currency

import forex.domain.currency.errors.CurrencyError
import io.circe.{ Decoder, Encoder }

sealed trait Currency
object Currency {

  case object AED extends Currency
  case object AFN extends Currency
  case object ALL extends Currency
  case object AMD extends Currency
  case object ANG extends Currency
  case object AOA extends Currency
  case object ARS extends Currency
  case object AUD extends Currency
  case object AWG extends Currency
  case object AZN extends Currency
  case object BAM extends Currency
  case object BBD extends Currency
  case object BDT extends Currency
  case object BGN extends Currency
  case object BHD extends Currency
  case object BIF extends Currency
  case object BMD extends Currency
  case object BND extends Currency
  case object BOB extends Currency
  case object BRL extends Currency
  case object BSD extends Currency
  case object BTN extends Currency
  case object BWP extends Currency
  case object BYN extends Currency
  case object BZD extends Currency
  case object CAD extends Currency
  case object CDF extends Currency
  case object CHF extends Currency
  case object CLP extends Currency
  case object CNY extends Currency
  case object COP extends Currency
  case object CRC extends Currency
  case object CUC extends Currency
  case object CUP extends Currency
  case object CVE extends Currency
  case object CZK extends Currency
  case object DJF extends Currency
  case object DKK extends Currency
  case object DOP extends Currency
  case object DZD extends Currency
  case object EGP extends Currency
  case object ERN extends Currency
  case object ETB extends Currency
  case object EUR extends Currency
  case object FJD extends Currency
  case object FKP extends Currency
  case object GBP extends Currency
  case object GEL extends Currency
  case object GGP extends Currency
  case object GHS extends Currency
  case object GIP extends Currency
  case object GMD extends Currency
  case object GNF extends Currency
  case object GTQ extends Currency
  case object GYD extends Currency
  case object HKD extends Currency
  case object HNL extends Currency
  case object HRK extends Currency
  case object HTG extends Currency
  case object HUF extends Currency
  case object IDR extends Currency
  case object ILS extends Currency
  case object IMP extends Currency
  case object INR extends Currency
  case object IQD extends Currency
  case object IRR extends Currency
  case object ISK extends Currency
  case object JEP extends Currency
  case object JMD extends Currency
  case object JOD extends Currency
  case object JPY extends Currency
  case object KES extends Currency
  case object KGS extends Currency
  case object KHR extends Currency
  case object KMF extends Currency
  case object KPW extends Currency
  case object KRW extends Currency
  case object KWD extends Currency
  case object KYD extends Currency
  case object KZT extends Currency
  case object LAK extends Currency
  case object LBP extends Currency
  case object LKR extends Currency
  case object LRD extends Currency
  case object LSL extends Currency
  case object LYD extends Currency
  case object MAD extends Currency
  case object MDL extends Currency
  case object MGA extends Currency
  case object MKD extends Currency
  case object MMK extends Currency
  case object MNT extends Currency
  case object MOP extends Currency
  case object MRU extends Currency
  case object MUR extends Currency
  case object MVR extends Currency
  case object MWK extends Currency
  case object MXN extends Currency
  case object MYR extends Currency
  case object MZN extends Currency
  case object NAD extends Currency
  case object NGN extends Currency
  case object NIO extends Currency
  case object NOK extends Currency
  case object NPR extends Currency
  case object NZD extends Currency
  case object OMR extends Currency
  case object PAB extends Currency
  case object PEN extends Currency
  case object PGK extends Currency
  case object PHP extends Currency
  case object PKR extends Currency
  case object PLN extends Currency
  case object PYG extends Currency
  case object QAR extends Currency
  case object RON extends Currency
  case object RSD extends Currency
  case object RUB extends Currency
  case object RWF extends Currency
  case object SAR extends Currency
  case object SBD extends Currency
  case object SCR extends Currency
  case object SDG extends Currency
  case object SEK extends Currency
  case object SGD extends Currency
  case object SHP extends Currency
  case object SLL extends Currency
  case object SOS extends Currency
  case object SPL extends Currency
  case object SRD extends Currency
  case object STN extends Currency
  case object SVC extends Currency
  case object SYP extends Currency
  case object SZL extends Currency
  case object THB extends Currency
  case object TJS extends Currency
  case object TMT extends Currency
  case object TND extends Currency
  case object TOP extends Currency
  case object TRY extends Currency
  case object TTD extends Currency
  case object TVD extends Currency
  case object TWD extends Currency
  case object TZS extends Currency
  case object UAH extends Currency
  case object UGX extends Currency
  case object USD extends Currency
  case object UYU extends Currency
  case object UZS extends Currency
  case object VEF extends Currency
  case object VND extends Currency
  case object VUV extends Currency
  case object WST extends Currency
  case object XAF extends Currency
  case object XCD extends Currency
  case object XDR extends Currency
  case object XOF extends Currency
  case object XPF extends Currency
  case object YER extends Currency
  case object ZAR extends Currency
  case object ZMW extends Currency
  case object ZWD extends Currency

  // Top 30 except USD most traded currencies
  val mostUsedCurrencies: List[Currency] = List(
    EUR,
    JPY,
    GBP,
    CNY,
    AUD,
    CAD,
    CHF,
    HKD,
    SGD,
    SEK,
    KRW,
    NOK,
    NZD,
    INR,
    MXN,
    TWD,
    ZAR,
    BRL,
    DKK,
    PLN,
    IDR,
    THB,
    TRY,
    ILS,
    CZK,
    HUF,
    AED,
    CLP,
    SAR,
    PHP
  )

  val leastUsedCurrencies: List[Currency] = List(
    AFN,
    ALL,
    AMD,
    ANG,
    AOA,
    ARS,
    AWG,
    AZN,
    BAM,
    BBD,
    BDT,
    BGN,
    BHD,
    BIF,
    BMD,
    BND,
    BOB,
    BSD,
    BTN,
    BWP,
    BYN,
    BZD,
    CDF,
    COP,
    CRC,
    CUC,
    CUP,
    CVE,
    DJF,
    DOP,
    DZD,
    EGP,
    ERN,
    ETB,
    FJD,
    FKP,
    GEL,
    GGP,
    GHS,
    GIP,
    GMD,
    GNF,
    GTQ,
    GYD,
    HNL,
    HRK,
    HTG,
    IMP,
    IQD,
    IRR,
    ISK,
    JEP,
    JMD,
    JOD,
    KES,
    KGS,
    KHR,
    KMF,
    KPW,
    KWD,
    KYD,
    KZT,
    LAK,
    LBP,
    LKR,
    LRD,
    LSL,
    LYD,
    MAD,
    MDL,
    MGA,
    MKD,
    MMK,
    MNT,
    MOP,
    MRU,
    MUR,
    MVR,
    MWK,
    MYR,
    MZN,
    NAD,
    NGN,
    NIO,
    NPR,
    OMR,
    PAB,
    PEN,
    PGK,
    PKR,
    PYG,
    QAR,
    RON,
    RSD,
    RUB,
    RWF,
    SBD,
    SCR,
    SDG,
    SHP,
    SLL,
    SOS,
    SPL,
    SRD,
    STN,
    SVC,
    SYP,
    SZL,
    TJS,
    TMT,
    TND,
    TOP,
    TTD,
    TVD,
    TZS,
    UAH,
    UGX,
    UYU,
    UZS,
    VEF,
    VND,
    VUV,
    WST,
    XAF,
    XCD,
    XDR,
    XOF,
    XPF,
    YER,
    ZMW,
    ZWD
  )

  val allCurrencies: List[Currency] = mostUsedCurrencies ++ leastUsedCurrencies

  private val Iso3 = "^[A-Za-z]{3}$".r

  def isMostUsed(currency: String): Boolean =
    mostUsedCurrencies.exists(_.toString == currency.toUpperCase)

  def fromString(s: String): Either[CurrencyError, Currency] =
    if (s.trim.isEmpty) Left(CurrencyError.Empty())
    else if (!Iso3.matches(s)) Left(CurrencyError.InvalidFormat())
    else {
      allCurrencies.find(_.toString == s.toUpperCase) match {
        case Some(c) => Right(c)
        case None    => Left(CurrencyError.Unsupported(s))
      }
    }

  def isMostUsed(currency: Currency): Boolean = mostUsedCurrencies.contains(currency)

  implicit val encoder: Encoder[Currency] = Encoder.encodeString.contramap(_.toString)
  implicit val decoder: Decoder[Currency] = Decoder.decodeString.emap(fromString(_).left.map(_.toString))
}
