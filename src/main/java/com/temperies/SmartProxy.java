package com.temperies;

import io.micrometer.core.instrument.util.IOUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

@WebServlet(
        name = "Tandil Proxy",
        description = "This project try to show us the request and response the we send and we recibe to the hotelplan supplier",
        urlPatterns = "/proxy/*"
)
@Slf4j
public class SmartProxy extends HttpServlet {

    public static final String CONTENT_TYPE = "application/xml";
    public static final String TEXT_XML_CHARSET_UTF_8 = "text/xml; charset=utf-8";
    @Value("${proxy.pre.url}")
    private String preURL;

    @Value("${proxy.new.url}")
    private String newURL;

    @Value("${basic.user}")
    private String user;

    @Value("${basic.password}")
    private String password;

    @Value("${dummies.location}")
    private String location;

    @Value("${save.response}")
    private boolean saveResponse;

    @Value("${use.dummies}")
    private boolean useDummies;

    private static int hits = 0;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String result = null;
        BufferedWriter writer = null;
        if (saveResponse){
            writer = new BufferedWriter(new FileWriter(location+ req.getRequestURL().toString().replace(preURL, "").replace("/",".").concat(".xml")));
        }
        result = HTTPConnector.callServiceByPost(null,
                newURL,
                30,
                CONTENT_TYPE,
                null,
                null,
                null);
        log.info("Response from service: {}",result);
        if (saveResponse){
            saveResponse(req, result, writer) ;
            writer.close();
        }
        resp.setContentType(TEXT_XML_CHARSET_UTF_8);
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(result);
        resp.getWriter().flush();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String body = getBody(req);

        log.info("Request: {}",body);
        String url = req.getRequestURL().toString().replace(preURL, newURL);

        String result = null;
        if (!useDummies){
            BufferedWriter writer = null;
            if (saveResponse){
                writer = new BufferedWriter(new FileWriter(location+ req.getRequestURL().toString().replace(preURL, "").replace("/",".").concat(".xml")));
            }
            try{
                result = HTTPConnector.callServiceByPost(body,
                        url,
                        1,
                        CONTENT_TYPE,
                        null,
                        null,
                        javax.xml.bind.DatatypeConverter.printBase64Binary(user.concat(":").concat(password).getBytes()));
            }catch (Exception ex){
                writer.close();
            }

            log.info("Response from service: {}",result);
            if (saveResponse){
                saveResponse(req, result, writer) ;
                writer.close();
            }
        }else{
            FileInputStream fileInputStream = new FileInputStream(location+ req.getRequestURL().toString().replace(preURL, "").replace("/",".").concat(hits+".xml"));
            result = IOUtils.toString(fileInputStream);
            log.info("Response from DUMMIES: {}",result);
        }

        resp.setContentType(TEXT_XML_CHARSET_UTF_8);
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(result);
        resp.getWriter().flush();
    }

    private void saveResponse(HttpServletRequest req, String body, BufferedWriter writer) throws IOException {
        writer.write(body);
    }

    private String getBody(HttpServletRequest req){
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = null;

        try {
            InputStream inputStream = req.getInputStream();
            if (inputStream != null) {
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                char[] charBuffer = new char[128];
                int bytesRead = -1;
                while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                    stringBuilder.append(charBuffer, 0, bytesRead);
                }
            } else {
                stringBuilder.append("");
            }
        } catch (IOException ex) {
            log.error("getBody {}", ex.getMessage());
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException ex) {
                    log.error("getBody {}", ex.getMessage());
                }
            }
        }
        return stringBuilder.toString();
    }

}
