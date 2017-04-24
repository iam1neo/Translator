package ru.iam1.translator;

import android.content.Context;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class PhpClient {
    private static String KEY="trnsl.1.1.20170419T194153Z.69e702a276d3a156.6df6c77104b7be215862cc5fd6d1c75a70c1c7d1";
    public static int RESP_OK = 0;
    public static int RESP_ERROR = 1;
    public static int RESP_NO_CONNECTION = 2;

    private Context context;

    public PhpClient(Context ctx){
        context = ctx;
    }

    private Document communicate(String api_url, String urlParameters){
        HttpsURLConnection conn;
        try {
            conn = (HttpsURLConnection) new URL(api_url).openConnection();
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
            wr.writeBytes("key="+KEY+"&"+urlParameters);
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

    //Получение списка поддерживаемых языков
    public String getLangs (){
        Document doc = communicate("https://translate.yandex.net/api/v1.5/tr/getLangs","ui="+Locale.getDefault().getLanguage());
        if(doc==null) return null;
        StringBuilder res = new StringBuilder();
        try {
            NodeList langsList = doc.getElementsByTagName("Item");
            for (int i = 0; i < langsList.getLength(); i++) {
                Element langEl = (Element) langsList.item(i);
                if(res.length()>0)res.append(";");
                res.append(langEl.getAttribute("key")+"="+langEl.getAttribute("value"));
            }
            return res.toString();
        }catch(Exception e){}
        return null;
    }

    //Получение списка поддерживаемых языков
    public String getTranslate(String text, String lang_code_from, String lang_code_to){
        String params;
        try {
            params = "text=" + URLEncoder.encode(text, "UTF-8");
            params += "&lang=" + lang_code_from +"-"+ lang_code_to;
            Document doc = communicate("https://translate.yandex.net/api/v1.5/tr/translate",params);
            if(doc==null) return null;
            return doc.getElementsByTagName("text").item(0).getTextContent();
        }catch(Exception e){}
        return null;
    }

    //Определение языка текста
    public String getDetectLangCode(String text){
        String params;
        try {
            params = "text=" + URLEncoder.encode(text, "UTF-8");
            Document doc = communicate("https://translate.yandex.net/api/v1.5/tr/detect",params);
            if(doc==null) return null;
            return ((Element)doc.getElementsByTagName("DetectedLang").item(0)).getAttribute("lang");
        }catch(Exception e){}
        return null;
    }
}
