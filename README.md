## Spring Kafka 多步驟請求-回應模式

本專案展示如何使用 Spring Kafka 實現一個涉及多個處理階段的同步請求-回應 (Request-Reply) 通訊模式。

### 專案概觀
[系統文件 by Deepwiki](./wiki/1-overview.html)

### 核心概念

在傳統的同步請求-回應模式中，客戶端發送請求，服務端處理並直接回應。此專案將其擴展，引入了中間處理步驟：

1.  **REST API (客戶端)**：位於 `restapi` 專案，接收 HTTP 請求。
2.  **第一階段處理 (`listener`)**：位於 `listener` 專案，監聽初始請求 Topic，執行第一步運算 (例如 `Math.pow`)。
3.  **第二階段處理 (`listener1`)**：位於 `listener1` 專案，監聽中間結果 Topic，執行第二步運算 (例如 `Math.sqrt`)。
4.  **回覆**：第二階段處理完成後，將最終結果發送回 REST API 指定的回覆 Topic。

![](img/system.png)  <!-- Assume a new image representing the multi-step flow -->

**流程詳述：**

1.  **客戶端發送請求**：`restapi` 使用 `ReplyingKafkaTemplate` 發送包含計算參數 (如 base 和 exponent) 的請求消息至 `rest-to-listener.request` Topic。請求消息包含 `KafkaHeaders.REPLY_TOPIC` (指定一個唯一的回覆 Topic) 和 `KafkaHeaders.CORRELATION_ID`。
2.  **`listener` 處理**：
    *   `listener` 專案中的 `NotificationDispatchListener` 監聽 `rest-to-listener.request` Topic。
    *   接收到請求後，執行 `Math.pow` 運算。
    *   將包含中間結果的 `CalculationResponse` 消息發送至 `listener-to-listener1.intermediate` Topic。**重要**: 此消息會轉發原始請求中的 `REPLY_TOPIC` 和 `CORRELATION_ID` header。
3.  **`listener1` 處理**：
    *   `listener1` 專案中的 `NotificationDispatchListener` 監聽 `listener-to-listener1.intermediate` Topic。
    *   接收到中間結果後，執行 `Math.sqrt` 運算。
    *   使用 `@SendTo` 將包含最終結果的 `NotificationDispatchResponse` 消息自動發送回消息 header 中指定的 `REPLY_TOPIC`。
4.  **客戶端接收回應**：`restapi` 在其指定的 Reply Topic 上接收到最終回應，解除阻塞，並將結果透過 HTTP 回傳給原始請求者。

**涉及的 Kafka Topics:**

*   `rest-to-listener.request`: REST API -> listener
*   `listener-to-listener1.intermediate`: listener -> listener1
*   回覆 Topic (動態產生，由 `ReplyingKafkaTemplate` 管理): listener1 -> REST API

**專案結構:**

*   `restapi/`: 包含 REST Controller 和 `ReplyingKafkaTemplate` 的客戶端應用。
*   `listener/`: 包含第一個 Kafka Listener (`Math.pow`)。
*   `listener1/`: 包含第二個 Kafka Listener (`Math.sqrt`)。

### 執行

1.  **啟動 Kafka 和 Zookeeper**：可以使用提供的 `docker-compose.yml`。
    ```bash
    docker-compose up -d
    ```
2.  **設置環境變數** (指向 Kafka Broker)：
    ```bash
    export REDPANDA_VERSION=25.1.1 
    export REDPANDA_CONSOLE_VERSION=3.0.0
    # 或根據你的 docker-compose.yml 設定
    ```
3.  **分別啟動應用**：
    *   啟動 `listener` 應用：
        ```bash
        cd listener
        mvn spring-boot:run 
        ```
    *   啟動 `listener1` 應用：
        ```bash
        cd ../listener1
        mvn spring-boot:run
        ```
    *   啟動 `restapi` 應用：
        ```bash
        cd ../restapi
        mvn spring-boot:run
        ```
4.  **發送請求**：使用 curl 或其他工具向 `restapi` 的端點發送請求，例如：
    ```bash
    curl -X POST http://localhost:8080/calculate -H "Content-Type: application/json" -d '{"base": 2.0, "exponent": 10.0}'
    ```
    預期會得到 `Math.sqrt(Math.pow(2.0, 10.0))` 的結果 (即 32.0)。


### 相關技術與參考

*   **Spring Kafka:** [官方文件](https://docs.spring.io/spring-kafka/reference/index.html)
*   **ReplyingKafkaTemplate:** 用於實現同步請求-回應。
*   **@KafkaListener & @SendTo:** 監聽 Topic 並回覆消息。
*   **Message Headers:** 使用 `KafkaHeaders.REPLY_TOPIC` 和 `KafkaHeaders.CORRELATION_ID` 進行路由和請求匹配。


### 其他相關文章

- [Synchronous Kafka: Using Spring Request-Reply](https://dzone.com/articles/synchronous-kafka-using-spring-request-reply-1)
- [Synchronous Communication With Apache Kafka Using ReplyingKafkaTemplate](https://www.baeldung.com/spring-kafka-request-reply-synchronous)
- [Spring Kafka: Configure Multiple Listeners on Same Topic](https://www.baeldung.com/spring-kafka-multiple-listeners-same-topic)
