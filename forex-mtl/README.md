#  Forex Proxy API

### Project overview: https://zeroford.notion.site/Project-Overview-Forex-Proxy-API-240376096cc680699424e629fde8ad82

> A Scala ProxyAPI service that fetches FX rates from OneFrame, caches them in-memory, and exposes a `/rates` endpoint. Designed for performance, fault tolerance, and testability.

## Getting started

### Prerequisites

- **Java 17** 
- **sbt 1.8.0+** or newer
- **Docker** (for Acceptance Test)


### Run API locally

Create `forex-mtl/.env`
```bash
ONE_FRAME_TOKEN=<your-token-here>
```
Then run application
```bash
cd forex-mtl
docker compose up -d
sbt run
```
---
## Run Unit tests
```bash
cd forex-mtl
sbt test
```

## Run Acceptance tests 
Create .env files before use
```bash
cd forex-mtl
docker compose -f docker-compose.acceptance.yml up -d
sbt acceptance:test
```

---

## API Usage


### > `GET` `http://localhost:8080/rates?from={Currency1}&to={Currency2}`

* `from`,`to`: ISO: 4217 currency codes (3 alphabetic characters).

```bash
# All support currencies (162 curreencies) are listed below
AED, AFN, ALL, AMD, ANG, AOA, ARS, AUD, AWG, AZN, BAM, BBD, BDT, BGN, BHD,
BIF, BMD, BND, BOB, BRL, BSD, BTN, BWP, BYN, BZD, CAD, CDF, CHF, CLP, CNY,
COP, CRC, CUC, CUP, CVE, CZK, DJF, DKK, DOP, DZD, EGP, ERN, ETB, EUR, FJD,
FKP, GBP, GEL, GGP, GHS, GIP, GMD, GNF, GTQ, GYD, HKD, HNL, HRK, HTG, HUF,
IDR, ILS, IMP, INR, IQD, IRR, ISK, JEP, JMD, JOD, JPY, KES, KGS, KHR, KMF,
KPW, KRW, KWD, KYD, KZT, LAK, LBP, LKR, LRD, LSL, LYD, MAD, MDL, MGA, MKD,
MMK, MNT, MOP, MRU, MUR, MVR, MWK, MXN, MYR, MZN, NAD, NGN, NIO, NOK, NPR,
NZD, OMR, PAB, PEN, PGK, PHP, PKR, PLN, PYG, QAR, RON, RSD, RUB, RWF, SAR,
SBD, SCR, SDG, SEK, SGD, SHP, SLL, SOS, SPL, SRD, STN, SVC, SYP, SZL, THB, 
TJS, TMT, TND, TOP, TRY, TTD, TVD, TWD, TZS, UAH, UGX, USD, UYU, UZS, VEF,
VND, VUV, WST, XAF, XCD, XDR, XOF, XPF, YER, ZAR, ZMW, ZWD
```
source: https://www.xe.com/currency/

### Response (200 OK) example:
```json
{
  "from": "USD",
  "to": "EUR",
  "price": 0.71,
  "timestamp": "2025-01-01T00:00:00.000Z"
}
```
|    HTTP | When                                                       |
| ------: | ---------------------------------------------------------- |
|     400 | Invalid/unsupported currency, or missing `from`/`to`       |
| 401/403 | Invalid/absent token when required for upstream            |
|     5xx | Upstream failure / internal error (mapped to server error) |
---

### `GET` `http://localhost:8080/health`

- Returns `200` when the service is healthy.
