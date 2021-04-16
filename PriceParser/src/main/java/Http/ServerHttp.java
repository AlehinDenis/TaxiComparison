package main.java.Http;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.io.*;
import java.util.List;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import main.java.PriceParser.*;

@WebServlet()
public class ServerHttp extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if(req.getQueryString() == null) {
            return;
        }
        ArrayList<String> requestParamValue = handleGetRequest(req);
        handleResponse(resp,requestParamValue);
    }

    private ArrayList<String> handleGetRequest(HttpServletRequest req) {
        String[] values = req.getQueryString().toString().split("\\?|=");
        ArrayList<String> result = new ArrayList<>();
        for(int i = 0; i < values.length; i++) {
            System.out.println(values[i]);
        }
            try {
                result.add(URLDecoder.decode(values[1],"utf8"));
                result.add(URLDecoder.decode(values[3],"utf8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

        return result;
    }

    private void handleResponse(HttpServletResponse resp, ArrayList<String> requestParamValue)  throws  IOException {
        PriceParser priceParser = new PriceParser(
                requestParamValue.get(0),
                requestParamValue.get(1));
        List<Pair<String, String>> prices = priceParser.run(100);
        String htmlResponse = "";
        for(int i = 0; i < prices.size(); i++) {
            htmlResponse +=
                    URLEncoder.encode(prices.get(i).getKey()
                            + ":"
                            + prices.get(i).getValue()
                            + ";","utf-8");
        }

        PrintWriter printWriter = resp.getWriter();
        printWriter.write(htmlResponse);
        printWriter.close();
        System.out.println(htmlResponse);
    }
}
