package org.servidor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Servidor {

    public static void main(String[] args) {
        Socket s = null;
        ExecutorService pool = Executors.newCachedThreadPool();

        try (ServerSocket ss = new ServerSocket(55555)){
            while (true){
                try{
                    s = ss.accept();

                    pool.execute(new AdministradorUsuarios(s));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            pool.shutdown();
        }
    }

}
