#  Forex Proxy API

Project overview: https://zeroford.notion.site/Project-Overview-Forex-Proxy-API-240376096cc680699424e629fde8ad82

A Scala ProxyAPI service that fetches FX rates from OneFrame, caches them in-memory, and exposes a `/rates` endpoint. Designed for performance, fault tolerance, and testability.


## Getting started

### Prerequisites

- Java 17
- sbt 1.8.0+
- Scala 2.13.12

### Installation

```bash
sbt run
```

---

## API Usage

Request example
```
http://localhost:8080/rates?pair=USDJPY
```
Success Response:

```json
{
  "from":"USD",
  "to":"JPY",
  "bid":0.61,
  "ask":0.82,
  "price":0.71,
  "time_stamp":"2019-01-01T00:00:00.000"
}
```

## Run Tests

```bash
sbt test
```

