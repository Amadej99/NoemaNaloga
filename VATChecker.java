import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;

import java.io.UnsupportedEncodingException;

public class VATChecker {
    public static void main(String[] args) throws UnsupportedEncodingException {
        Scanner sc = new Scanner(System.in);

        HashMap<List<Integer>, Integer> slovenianRegion = new HashMap<>();

        slovenianRegion.put(Arrays.asList(196, 346), 268);
        slovenianRegion.put(Arrays.asList(196, 8224), 262);
        slovenianRegion.put(Arrays.asList(313, 733), 381);
        slovenianRegion.put(Arrays.asList(313, 160), 352);

        // 196 + 346 -> Č (268)
        // 196 + 8224 -> Ć (262)
        // 313 + 733 -> Ž (381)
        // 313 + 160 -> Š (352)

        while (true) {
            System.out.print(translate("VNESI DAVČNO ŠTEVILKO V FORMATU SI XXXXXXX: ".toCharArray(), slovenianRegion));

            String input = sc.nextLine();

            // Odstranim morebitne presledke ter pretvorim morebitne male crke v velike.
            input = input.replace(" ", "").toUpperCase();

            // Z regularnimi izrazi preverim veljavnost vhoda. Dolocene drzave imajo
            // druagacne formate, zato svoji regularni izrazi.
            Pattern generalPattern = Pattern.compile("^[A-Z]{2}[0-9]+$");
            Pattern ATPattern = Pattern.compile("^[A-Z]{3}[0-9]+$");
            Pattern BEIEESPattern = Pattern.compile("^[A-Z]{2}[0-9]+[A-Z]+$");
            Pattern NLPattern = Pattern.compile("^[A-Z]{2}[0-9]+[A-Z]+[0-9]{2}$");

            Pattern[] regexCheck = { generalPattern, ATPattern, BEIEESPattern, NLPattern };

            boolean match = false;
            for (Pattern check : regexCheck) {
                if (check.matcher(input).find()) {
                    match = true;
                }
            }

            String countryCode;
            String VATNr;

            // Ce je vhod veljaven, klicem web storitev.
            if (match) {
                countryCode = input.substring(0, 2);
                VATNr = input.substring(2, input.length());

                HashMap<String, String> response = getVATDetails(countryCode, VATNr);

                String fault = response.get("fault");

                // Preverim morebitne napake ob klicu storitve, ce pride do napake, ponovno
                // prosim za vhod.
                if (fault != null) {
                    if (fault.equals("noInternet")) {
                        System.out.println("VZPOSTAVI POVEZAVO Z INTERNETOM!");
                    } else {
                        System.out.println(translate("NAPAČNA OZNAKA DRŽAVE / NAPAKA NA STREŽNIKU.".toCharArray(),
                                slovenianRegion));
                    }
                    continue;
                }

                // Preverim, ali je podjetje davcni zavezanec in prekinem zanko, saj smo dobili
                // veljavni vhod.
                if (response.get("valid").equals("false")) {
                    System.out.println(translate("PODJETJE NI DAVČNI ZAVEZANEC.".toCharArray(), slovenianRegion));
                } else {

                    char[] name = response.get("name").toCharArray();
                    char[] address = response.get("address").toCharArray();

                    String translatedName = translate(name, slovenianRegion);
                    String translatedAddress = translate(address, slovenianRegion);

                    System.out.println("--------------------------------------------------------------");
                    System.out.println(translatedName);
                    System.out.println(translatedAddress);
                    System.out.println("--------------------------------------------------------------");

                }
                sc.close();
                break;
            }

            System.out.println(translate("VNESI VELJAVNO DAVČNO ŠTEVILKO!".toCharArray(), slovenianRegion));
        }
    }

    // Prevede napacne crke v odgovoru v pravilne slovenske.
    public static String translate(char[] sequence, HashMap<List<Integer>, Integer> region) {
        char[] newSequence = sequence;
        for (int i = 0; i < sequence.length - 1; i++) {
            int code = (int) sequence[i];
            int nextCode = (int) sequence[i + 1];
            for (Map.Entry<List<Integer>, Integer> group : region.entrySet()) {
                if (group.getKey().contains(code) && group.getKey().contains(nextCode)) {
                    int newCode = group.getValue();
                    newSequence[i] = (char) newCode;
                    newSequence[i + 1] = '?';
                    i++;
                }
            }
        }
        String newString = new String(newSequence).replace("?", "");
        return newString;
    }

    public static HashMap<String, String> getVATDetails(String country, String VATNr) {
        // URL ter obrazec za poizvedbo
        String API = "http://ec.europa.eu/taxation_customs/vies/services/checkVatService";
        String xmlInput = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:urn=\"urn:ec.europa.eu:taxud:vies:services:checkVat:types\">"
                +
                "<soapenv:Header/>" +
                "<soapenv:Body>" +
                "<urn:checkVat>" +
                "<urn:countryCode>" + country + "</urn:countryCode>" +
                "<urn:vatNumber>" + VATNr + "</urn:vatNumber>" +
                "</urn:checkVat>" +
                "</soapenv:Body>" +
                "</soapenv:Envelope>";

        HashMap<String, String> response = new HashMap<String, String>();

        try {
            // Vzpostavi povezavo
            URL url = new URL(API);
            URLConnection connection = url.openConnection();
            HttpURLConnection httpConn = (HttpURLConnection) connection;

            byte[] buffer = xmlInput.getBytes();

            // Nastavi HTTP headerje
            httpConn.setRequestProperty("Content-Length", String
                    .valueOf(buffer.length));
            httpConn.setRequestProperty("Content-Type",
                    "text/xml; charset=utf-8");

            httpConn.setRequestMethod("POST");
            httpConn.setDoOutput(true);
            httpConn.setDoInput(true);

            // Poslje poizvedbo
            OutputStream out = httpConn.getOutputStream();
            out.write(buffer);
            out.close();

            // Prebere odgovor
            InputStreamReader isr = new InputStreamReader(httpConn.getInputStream());
            BufferedReader in = new BufferedReader(isr);

            String responseString = in.readLine();

            // Pretvori XML odgovor v Document objekt.
            Document document = parseXmlFile(responseString);

            // Pregleda za napake. Ce obstajajo v slovar doda napako in ga vrne.
            try {
                NodeList faultNode = document.getElementsByTagName("faultcode");
                String fault = faultNode.item(0).getTextContent();

                response.put("fault", fault);
                return response;

            } catch (Exception e) {
                ;
            }

            // Iz Document objekta dobi potrebne podatke o davcnem zavezancu.
            NodeList validNode = document.getElementsByTagName("ns2:valid");
            String validity = validNode.item(0).getTextContent();

            NodeList nameNode = document.getElementsByTagName("ns2:name");
            String name = nameNode.item(0).getTextContent();

            NodeList addressNode = document.getElementsByTagName("ns2:address");
            String address = addressNode.item(0).getTextContent();

            // V slovar doda kljuc valid, ki nam pove ali je podjetje davcni zavezanec. Ce
            // ni, tak slovar vrnem.
            response.put("valid", validity);
            // Ce podjetje je davcni zavezanec, v slovar dodam se ime in naslov.
            if (validity.equals("true")) {
                response.put("name", name);
                response.put("address", address);
            }

        } catch (Exception e) {
            // Sklepam, da je napaka nastala zaradi pomanjkanja internetne povezave in
            // ponovno prosim za vhod.
            response.put("fault", "noInternet");
            return response;
            // e.printStackTrace();
        }

        // Narejeni slovar vrnem.
        return response;
    }

    private static Document parseXmlFile(String in) {
        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(in));
            return db.parse(is);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}