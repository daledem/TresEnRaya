package org.servidor;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.Socket;

public class AdministradorUsuarios extends Thread{

    private Socket s;

    public AdministradorUsuarios(Socket s){
        this.s = s;
    }

    public void run(){
        String mensaje;
        String [] resultado;
        String ip;
        String nombre;
        int puerto;
        int elo;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(this.s.getInputStream())); PrintStream ps = new PrintStream(this.s.getOutputStream())){

            mensaje = br.readLine();

            if (mensaje.startsWith("RESULTADO ")){ // Si el mensaje debera ser de la forma 'RESULTADO <nombre> <puntos>
                                                   // donde puntos es la cantidad de puntos ganados o perdidos
                resultado = mensaje.split(" ");

                if (resultado.length == 3){
                    elo = Integer.parseInt(resultado[2]);
                    nombre = resultado[1];
                    if (sumarElo(nombre,elo)){
                        this.mensajeOk(ps);
                    }else {
                        this.mensajeError(ps);
                    }

                }else {
                    this.mensajeError(ps);
                }
            }else {
                resultado = mensaje.split(" "); // El mensaje debe contener el nombre seguido de la IP

                if (resultado.length == 2){
                    nombre = resultado[0];
                    ip = resultado[1];

                    if (buscarUsuario(nombre,ip)){ // Si el usuario no se encuentra en el xml, se registrara
                        aniadirUsuario(nombre,ip);
                    }

                    do {

                        enviarMesas(ps);

                        mensaje = br.readLine();

                        if (mensaje.startsWith("NUEVAMESA")) {
                            // Si desea crear una nueva, el usuario enviara
                            // NUEVAMESA <puerto>, siendo puerto el puerto en el
                            // que el usuario hosteara la mesa

                            resultado = mensaje.split(" ");
                            if (resultado.length == 2) {
                                puerto = Integer.parseInt(resultado[1]);
                                aniadirMesa(nombre, puerto);
                            } else {
                                mensajeError(ps);
                            }
                        } else if(!mensaje.equals("ACTUALIZAR")){
                            // En caso de que no desee crear una mesa sino unirse, simplemente enviara el nombre
                            // de la persona a la que quiere unirse

                            if (enviarUsuarioEnMesa(mensaje, ps)) {
                                quitarMesa(mensaje);
                            } else {
                                mensajeError(ps);
                            }
                        }
                    }while (mensaje.equals("ACTUALIZAR"));

                }else {
                    mensajeError(ps);
                }

            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }finally {
            try {
                if (this.s != null){
                    this.s.close();
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }


    }

    private boolean sumarElo(String nombre,int variacionElo){
        DocumentBuilderFactory dbf = null;
        DocumentBuilder db = null;
        Document doc = null;
        int nuevoElo;
        boolean sumado = false;

        try {
            dbf = DocumentBuilderFactory.newInstance();
            db = dbf.newDocumentBuilder();
            doc = db.parse(new File("src/main/xml/Usuarios.xml"));

            NodeList usuarios = doc.getElementsByTagName("usuario");

            int i = 0;
            int lenght = usuarios.getLength();
            while (i<lenght && !sumado ){
                Element usuario = (Element) usuarios.item(i);

                if (usuario.getAttributeNode("nombre").getValue().equals(nombre)){
                    Element elo = (Element) usuario.getElementsByTagName("elo");

                    nuevoElo = Integer.parseInt(elo.getNodeValue()) + variacionElo;
                    elo.setNodeValue(String.valueOf(nuevoElo));
                    sumado = true;
                }
                i++;
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File("src/main/xml/Usuarios.xml"));
            transformer.transform(source, result);

        } catch (ParserConfigurationException | IOException | SAXException | TransformerException e) {
            e.printStackTrace();
        }

        return sumado;
    }

    private void mensajeError(PrintStream ps){
        ps.println("ERROR");
    }

    private void mensajeOk(PrintStream ps){
        ps.println("OK");
    }

    private boolean buscarUsuario(String nombre,String ip){
        DocumentBuilderFactory dbf = null;
        DocumentBuilder db = null;
        Document doc = null;
        boolean encontrado = false;

        try {
            dbf = DocumentBuilderFactory.newInstance();
            db = dbf.newDocumentBuilder();
            doc = db.parse(new File("src/main/xml/Usuarios.xml"));

            NodeList usuarios = doc.getElementsByTagName("usuario");

            int i = 0;
            int lenght = usuarios.getLength();
            while (i<lenght && !encontrado ){
                Element usuario = (Element) usuarios.item(i);

                if (usuario.getAttributeNode("nombre").getValue().equals(nombre)){

                    if (!usuario.getAttributeNode("ip").getValue().equals(ip)){
                        usuario.getAttributeNode("ip").setValue(ip);
                    }
                    encontrado = true;
                }
                i++;
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File("src/main/xml/Usuarios.xml"));
            transformer.transform(source, result);



        } catch (ParserConfigurationException | IOException | SAXException | TransformerException e) {
            e.printStackTrace();
        }

        return encontrado;
    }

    private void enviarMesas(PrintStream ps){
        DocumentBuilderFactory dbf = null;
        DocumentBuilder db = null;
        Document doc = null;
        String elo;
        String nombre;
        boolean esperandoEnMesa;

        try {
            dbf = DocumentBuilderFactory.newInstance();
            db = dbf.newDocumentBuilder();
            doc = db.parse(new File("src/main/xml/Usuarios.xml"));

            NodeList usuarios = doc.getElementsByTagName("usuario");

            int i = 0;
            int lenght = usuarios.getLength();
            while (i<lenght){
                Element usuario = (Element) usuarios.item(i);
                NodeList mesa = usuario.getElementsByTagName("*");

                esperandoEnMesa = mesa.getLength() == 2;

                if (esperandoEnMesa){
                    elo = mesa.item(0).getNodeValue();
                    nombre = usuario.getAttributeNode("nombre").getValue();

                    ps.println(nombre + " " + elo);
                }
                i++;
            }

            ps.println("FIN");
        } catch (ParserConfigurationException | IOException | SAXException e) {
            e.printStackTrace();
        }
    }

    private boolean aniadirUsuario(String nombre, String ip){
        DocumentBuilderFactory dbf = null;
        DocumentBuilder db = null;
        Document doc = null;
        boolean creado = false;
        Element nuevoUsuario = null;
        Element elo = null;
        Text txtElo = null;

        try {
            dbf = DocumentBuilderFactory.newInstance();
            db = dbf.newDocumentBuilder();
            doc = db.parse(new File("src/main/xml/Usuarios.xml"));

            nuevoUsuario = doc.createElement("usuario");
            nuevoUsuario.setAttribute("nombre",nombre);
            nuevoUsuario.setAttribute("ip",ip);

            elo = doc.createElement("elo");
            txtElo = doc.createTextNode("850");
            elo.appendChild(txtElo);

            nuevoUsuario.appendChild(elo);

            doc.getDocumentElement().appendChild(nuevoUsuario);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File("src/main/xml/Usuarios.xml"));
            transformer.transform(source, result);

            creado = true;

        } catch (ParserConfigurationException | IOException | SAXException | TransformerException e) {
            e.printStackTrace();
        }

        return creado;
    }


    private boolean enviarUsuarioEnMesa(String nombre, PrintStream ps){
        DocumentBuilderFactory dbf = null;
        DocumentBuilder db = null;
        Document doc = null;
        String puerto;
        String nombreXml;
        String ip;
        boolean enviado = false;
        boolean esperandoEnMesa;

        try {
            dbf = DocumentBuilderFactory.newInstance();
            db = dbf.newDocumentBuilder();
            doc = db.parse(new File("src/main/xml/Usuarios.xml"));

            NodeList usuarios = doc.getElementsByTagName("usuario");

            int i = 0;
            int lenght = usuarios.getLength();
            while (i<lenght && !enviado){
                Element usuario = (Element) usuarios.item(i);
                NodeList mesa = usuario.getElementsByTagName("*");

                nombreXml = usuario.getAttributeNode("nombre").getValue();
                esperandoEnMesa = mesa.getLength() == 2;

                if (esperandoEnMesa && nombreXml.equals(nombre)){
                    puerto = usuario.getAttributeNode("puerto").getValue();
                    ip = usuario.getAttributeNode("ip").getValue();

                    ps.println(ip + " " + puerto);
                    enviado = true;
                }
                i++;
            }

            ps.println("FIN");
        } catch (ParserConfigurationException | IOException | SAXException e) {
            e.printStackTrace();
        }
        return enviado;
    }

    private boolean aniadirMesa(String nombre,int puerto){
        DocumentBuilderFactory dbf = null;
        DocumentBuilder db = null;
        Document doc = null;
        Element esperandoEnMesa;
        boolean aniadida = false;

        try {
            dbf = DocumentBuilderFactory.newInstance();
            db = dbf.newDocumentBuilder();
            doc = db.parse(new File("src/main/xml/Usuarios.xml"));

            NodeList usuarios = doc.getElementsByTagName("usuario");

            int i = 0;
            int lenght = usuarios.getLength();
            while (i<lenght && !aniadida){
                Element usuario = (Element) usuarios.item(i);

                if (usuario.getAttributeNode("nombre").getValue().equals(nombre)){
                    usuario.setAttribute("puerto",String.valueOf(puerto));

                    esperandoEnMesa = doc.createElement("esperandoEnMesa");
                    usuario.appendChild(esperandoEnMesa);

                    aniadida = true;
                }
                i++;
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File("src/main/xml/Usuarios.xml"));
            transformer.transform(source, result);

        } catch (ParserConfigurationException | IOException | SAXException | TransformerException e) {
            e.printStackTrace();
        }
        return aniadida;
    }

    private boolean quitarMesa(String nombre){
        DocumentBuilderFactory dbf = null;
        DocumentBuilder db = null;
        Document doc = null;
        boolean eliminada = false;

        try {
            dbf = DocumentBuilderFactory.newInstance();
            db = dbf.newDocumentBuilder();
            doc = db.parse(new File("src/main/xml/Usuarios.xml"));

            NodeList usuarios = doc.getElementsByTagName("usuario");

            int i = 0;
            int lenght = usuarios.getLength();
            while (i<lenght && !eliminada){
                Element usuario = (Element) usuarios.item(i);

                if (usuario.getAttributeNode("nombre").getValue().equals(nombre)){
                    usuario.removeAttribute("puerto");

                    usuario.removeChild(doc.createElement("esperandoEnMesa"));

                    eliminada = true;
                }
                i++;
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File("src/main/xml/Usuarios.xml"));
            transformer.transform(source, result);

        } catch (ParserConfigurationException | IOException | SAXException | TransformerException e) {
            e.printStackTrace();
        }
        return eliminada;
    }

}