package ru.iam1.translator;

import android.content.Context;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

public class PhpClient {
    public static int RESP_OK = 0;
    public static int RESP_ERROR = 1;
    public static int RESP_NO_CONNECTION = 3;

    private Context context;

    public PhpClient(Context ctx){
        context = ctx;
    }

    private void log(Object o){
        System.out.println(o);
    }

    private Document communicate(String api_url, String urlParameters){
        HttpURLConnection conn;
        try {
            conn = (HttpURLConnection) new URL(api_url).openConnection();
        }catch(Exception e){
            return null;
        }
        try {
            conn.setConnectTimeout(context.getResources().getInteger(R.integer.server_connect_timeout_ms));
            conn.setReadTimeout(context.getResources().getInteger(R.integer.server_read_timeout_ms));
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);

            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            String buffer;
            StringBuffer response = new StringBuffer();

            while ((buffer = in.readLine()) != null) {
                response.append(buffer);
            }
            in.close();
            String res = response.toString();

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(res));
            return builder.parse(is);
        }catch(Exception e) {
            try{
                conn.disconnect();
            }catch(Exception e2){}
            log("error "+e.toString());
        }
        return null;
    }

    public String printXmlDocument(Document doc) {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            return writer.getBuffer().toString();//.replaceAll("\n|\r", "");
        }catch(Exception e){}
        return null;
    }

    public int test_connection(){
        return RESP_OK;
        /*Document xml_res = communicate("http://192.168.0.20:8080/android_server.php","api=1.0&action=test_connection");
        if(xml_res==null) return RESP_NO_CONNECTION;
        try {
            XPathFactory xpf = XPathFactory.newInstance();
            XPath xp = xpf.newXPath();
            String code = xp.evaluate("/DetectedLang/@code", xml_res.getDocumentElement());
            String lang = xp.evaluate("/DetectedLang/@lang",xml_res.getDocumentElement());
            log("code = "+code);
            log("lang = "+lang);
            if(code.equals("200")) return RESP_OK;
        }catch(Exception e){}
        return RESP_NO_CONNECTION;//"Нет соединения с сервером."*/
    }
}
