package com.okcoin.commons.okex.open.api.examples.futures.strategies;

import com.alibaba.fastjson.JSONObject;
import com.okcoin.commons.okex.open.api.bean.futures.param.Order;
import com.okcoin.commons.okex.open.api.bean.futures.result.OrderResult;
import com.okcoin.commons.okex.open.api.config.APIConfiguration;
import com.okcoin.commons.okex.open.api.examples.futures.connectivity.WSAdaptor;
import com.okcoin.commons.okex.open.api.examples.futures.interfaces.FuturesOrderManager;
import com.okcoin.commons.okex.open.api.examples.futures.interfaces.MarketUpdateListener;
import com.okcoin.commons.okex.open.api.examples.futures.models.OrderAck;
import com.okcoin.commons.okex.open.api.service.futures.FuturesTradeAPIService;
import com.okcoin.commons.okex.open.api.service.futures.impl.FuturesTradeAPIServiceImpl;
import com.okcoin.commons.okex.open.api.websocket.WebSocketClient;

import java.util.Properties;

/**
 *
 * Separting out the exchange specific logic into helper logic which can be added as a
 */
public abstract class BaseStrategy implements MarketUpdateListener, FuturesOrderManager {

    private FuturesTradeAPIService tradeAPIService;
    private WebSocketClient wsclient;
    private String exchange;

    public BaseStrategy(Properties properties) {
        String exchange = properties.get("exchange").toString();
        this.exchange = exchange;
        if(exchange.equals("okex")) {
            APIConfiguration config = new APIConfiguration();
            config.setEndpoint(properties.get("rest-url").toString());
            config.setApiKey(properties.get("api-key").toString());
            config.setSecretKey(properties.get("secret-key").toString());
            config.setPassphrase(properties.get("pass-phrase").toString());
            config.setPrint(false);

            wsclient = new WebSocketClient(properties.get("websocket-url").toString(), new WSAdaptor(this));
            wsclient.connect();
            wsclient.login(config.getApiKey(), config.getSecretKey(), config.getPassphrase());
            this.tradeAPIService = new FuturesTradeAPIServiceImpl(config);
        } else if(exchange.equals("test")) {
            System.out.println("TEST EXCHANGE...");
        }
    }

    public OrderAck sendOrder(String type, String instrumentId, double price, int quantity, double matchPrice) {
        String clOrdId = "oid"+((int)(Math.random()*100000));
        Order order = new Order();
        order.setClient_oid(clOrdId);
        order.setType(type);
        order.setInstrument_id(instrumentId);
        order.setPrice(Double.toString(price));
        order.setSize(Integer.toString(quantity));
        order.setMatch_price(Double.toString(matchPrice));
        System.out.println("Sending order- clOrdId:"+clOrdId+", instrumentId:"+instrumentId+", type :" +type+", price: "+price+", quantity: "+quantity);
        OrderResult result = tradeAPIService.order(order);
        System.out.println("Order Ack'ed - OrderId:"+result.getOrder_id()+", clOrdId:"+clOrdId+", instrumentId:"+instrumentId+", type :" +type+", price: "+price+", quantity: "+quantity);
        return new OrderAck(result);
    }

    public JSONObject cancelOrder(String instrumentId, String orderId) {
        return tradeAPIService.cancelOrderByOrderId(instrumentId, orderId);
    }

    public void subscribe(String... channels) {
        wsclient.subscribe(channels);
    }

    public void unsubscribe(String channel) {
        wsclient.unSubscribe(channel);
    }

    @Override
    public FuturesTradeAPIService getTradeService() {
        return this.tradeAPIService;
    }

    public abstract void run();

    public void destroy() {
        System.out.println("Exiting strategy ...");
        wsclient.close();
    }
}
