package myWebServer;

import java.io.*;
import java.net.*;
import java.util.*;

public class WebServer implements Runnable
{
    static final int port=8080;

    static final File WEB_ROOT = new File("C:/JAVA/SocketProram");
    static final String DEFAULT_FILE = "index.html";
    static final String IMAGE_FILE = "images.html";

    private Socket clientConnect;

    public WebServer(Socket c)
    {
        clientConnect=c;
    }

    static boolean check=true;

    public static void main(String[] args) throws IOException {
        try
        {
            ServerSocket serverConnect = new ServerSocket(port);
            System.err.println("Server started.\nListening for connections on port : " + port + " ...\n");
            while (true) {
                WebServer myServer = new WebServer(serverConnect.accept());
                if (check) {
                    System.out.println("Connection opened. (" + new Date() + ")");
                }
                Thread thread = new Thread(myServer);
                thread.start();
                /*if (check == false) {
                    serverConnect.close();
                    break;
                }*/
                serverConnect.close();
            }
        }
        catch(IOException e)
        {
            System.err.println("Server Connection : " + e.getMessage());
        }
        /*finally {
            System.out.println("Server has been shutdown!");
        }*/

    }


     public void run() {

        BufferedReader in = null;
        PrintWriter out = null;
        BufferedOutputStream dataOut = null;
        String fileRequested = null;

        try {
            in = new BufferedReader(new InputStreamReader(clientConnect.getInputStream()));
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(clientConnect.getOutputStream())),
                    true);
            dataOut = new BufferedOutputStream(clientConnect.getOutputStream());

            String clientRequest = in.readLine();
            String method = clientRequest.substring(0,clientRequest.indexOf(" "));
            fileRequested = clientRequest.substring(clientRequest.indexOf(" ")+1,clientRequest.indexOf(" ",clientRequest.indexOf(" ")+1));


                if (!method.equals("GET") && !method.equals("POST")) {
                    if (check) {
                        System.out.println("501 Not Implemented : " + method + " method.");
                    }
                    check=false;

                } else if (method.equals("GET")) {
                    if (fileRequested.endsWith("/")) {
                        fileRequested += DEFAULT_FILE;
                    }
                    File file = new File(WEB_ROOT, fileRequested);
                    int fileLength = (int) file.length();
                    String content = getContentType(fileRequested);
                    byte[] fileData = readFileData(file, fileLength);
                    out.write("HTTP/1.1 200 OK \r\n");
                    out.write("Content-type: " + content + "\r\n");
                    out.write("Content-length: \r\n");
                    out.flush();

                    dataOut.write(fileData, 0, fileLength);
                    dataOut.flush();

                    if (check) {
                        System.out.println("File " + fileRequested + " of type " + content + " returned with get method ");
                    }
                } else if (method.equals("POST")) {

                    //System.out.println("post method");

                    String body=getBodyRequest(in);
                    //System.out.println(body);
                    fileRequested += IMAGE_FILE;
                    String uname=body.substring(6,body.indexOf('&'));
                    String psw=body.substring(body.indexOf('&')+7,body.length());
                    //System.out.println(uname+" "+psw);
                    File file = new File(WEB_ROOT, fileRequested);
                    int fileLength = (int) file.length();
                    String content = getContentType(fileRequested);
                    byte[] fileData = readFileData(file, fileLength);

                    if (uname.equals("admin") && (psw.equals("123456"))) {
                        // send HTTP Headers
                        out.write("HTTP/1.1 200 OK \r\n");
                        //out.println("Date: " + new Date());
                        out.write("Content-type: " + content + "\r\n");
                        out.write("Content-length: \r\n" + fileLength + "\r\n");
                        out.write("\r\n"); // blank line between headers and content
                        out.flush();

                        dataOut.write(fileData, 0, fileLength);
                        dataOut.flush();

                        if (check) {
                            System.out.println("File " + fileRequested + " of type " + content + " returned with post method");
                            check=false;
                        }
                    }
                    else {
                        try
                        {
                            fileUnauthorized(out,fileRequested);
                        }
                        catch (IOException ex)
                        {
                            System.err.println("Error with file unauthorized exception : " + ex.getMessage());
                        }

                    }
                }
        }catch (FileNotFoundException fnf)
            {
                try
                {
                    fileNotFound(out,fileRequested);
                }
                catch (IOException ex)
                {
                    System.err.println("Error with file not found exception : " + ex.getMessage());
                }
            }
        catch(IOException e)
        {
            System.err.println("Server error : " + e.getMessage());
        }
        finally
        {
            try
            {
                in.close();
                out.close();
                dataOut.close();
                clientConnect.close();
            }
            catch (IOException ioe)
            {
                System.err.println("Error closing stream : " + ioe.getMessage());
            }

        }
    }

    private String getBodyRequest(BufferedReader in) throws IOException {
        char[] body=new char[1024];
        in.read(body, 0, 1024);
        String bd="";
        for (int i:body)
        {
            if ((char)i!='\0') {
                bd += (char) i;
            }
        }
        bd=bd.substring(bd.indexOf("fname"),bd.length());
        //System.out.println(bd);
        return bd;
    }

    public String read(InputStream inputStream) throws IOException {
        StringBuilder result = new StringBuilder();
        do {
            result.append((char) inputStream.read());
        } while (inputStream.available() > 0);
        return result.toString();
    }

    private String getContentType(String fileRequested) {
        if (fileRequested.endsWith(".hml")  ||  fileRequested.endsWith(".html"))
        {
            return "text/html";
        }
        else if (fileRequested.endsWith(".txt"))
        {
            return "text/plain";
        }
        else if (fileRequested.endsWith(".jpg")  ||  fileRequested.endsWith(".jpeg"))
        {
            return "image/jpeg";
        }
        else if (fileRequested.endsWith(".gif"))
        {
            return "image/gif";
        }
        else if (fileRequested.endsWith(".png"))
        {
            return "image/png";
        }
        else if (fileRequested.endsWith(".css"))
        {
            return "text/css";
        }
        else return "application/octet-stream";
    }

    private byte[] readFileData(File file, int fileLength) throws IOException {
        FileInputStream fileIn = null;
        byte[] fileData = new byte[fileLength];

        try {
            fileIn = new FileInputStream(file);
            fileIn.read(fileData);
        } finally {
            if (fileIn != null)
                fileIn.close();
        }

        return fileData;
    }

    private void fileNotFound(PrintWriter out,  String fileRequested) throws IOException {
        String content = "text/html";

        out.write("HTTP/1.1 404 File Not Found \r\n");
        out.write("Content-type: " + content + "\r\n");
        out.write("\r\n"); // blank line between headers and content
        out.write("<!DOCTYPE html><html><head><title>404 Not Found</title></head>" +
                "<body><p>The requested file cannot be found.</p></body></htmml>");
        out.flush();
        if (check) {
            System.out.println("File " + fileRequested + " not found");
        }
        //check=false;
    }

    private void fileUnauthorized(PrintWriter out,String fileRequested) throws IOException{
        String content = "text/html";

        out.write("HTTP/1.1 401 Unauthorized \r\n");
        out.write("Content-type: " + content + "\r\n");
        out.write("\r\n"); // blank line between headers and content
        out.write("<!DOCTYPE html><html><head><title>This is a private area.</title></head>" +
                "<body><p></p></body></htmml>");
        out.flush();
        if (check) {
            System.out.println("File "+ fileRequested+ " unauthorized");
        }
        check=false;
    }

}