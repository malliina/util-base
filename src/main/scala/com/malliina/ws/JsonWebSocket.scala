package com.malliina.ws

import play.api.libs.json.JsValue

/**
 *
 * @author mle
 */
trait JsonWebSocket extends WebSocketBase[JsValue]
