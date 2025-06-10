package com.mycompany.servidorsemaforo;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class PrimaryController {

    @FXML
    private TextField txtIp;

    @FXML
    private TextField txtPorta;

    @FXML
    private Button btnParar;

    @FXML
    private TextArea txtLog;

    private ServerSocket serverSocket;
    private boolean servidorAtivo = false; // ← controla o estado
    private Thread servidorThread;

    @FXML
private void initialize() {
    try {
        String ipLocal = java.net.InetAddress.getLocalHost().getHostAddress();
        txtIp.setText(ipLocal);
    } catch (Exception e) {
        txtIp.setText("Erro ao obter IP");
    }

    // Desativar campo da porta após iniciar
    txtPorta.setDisable(true);

    // Muda o texto e estilo do botão
    btnParar.setText("Parar Servidor");
    btnParar.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");

    // Iniciar servidor automaticamente
    iniciarServidor();
}


    @FXML
    private void alternarServidor() {
        if (servidorAtivo) {
            pararServidor();
        } else {
            iniciarServidor();
        }
    }

    private void iniciarServidor() {
        int porta;

        try {
            porta = Integer.parseInt(txtPorta.getText());
        } catch (NumberFormatException e) {
            escreverLog("Porta inválida!");
            return;
        }

        escreverLog("A iniciar servidor na porta " + porta + "...");

        servidorThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(porta);
                servidorAtivo = true;
                atualizarBotao("Parar Servidor", "#e74c3c");

                escreverLog("Servidor iniciado. À espera de clientes...");

                while (!serverSocket.isClosed()) {
                    Socket socket = serverSocket.accept();
                    escreverLog("Cliente conectado: " + socket.getInetAddress());

                    Thread clienteThread = new Thread(() -> tratarCliente(socket));
                    clienteThread.start();
                }

            } catch (IOException e) {
                escreverLog("Servidor encerrado ou erro: " + e.getMessage());
            }
        });

        servidorThread.setDaemon(true);
        servidorThread.start();
    }

    private void pararServidor() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                escreverLog("Servidor encerrado com sucesso.");
            }
        } catch (IOException e) {
            escreverLog("Erro ao fechar o servidor: " + e.getMessage());
        }

        servidorAtivo = false;
        atualizarBotao("Iniciar Servidor", "#3498db");
    }

    private void tratarCliente(Socket socket) {
        try {
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            String msg = dis.readUTF();
            escreverLog("Mensagem recebida: " + msg);
            dis.close();
            socket.close();
        } catch (IOException e) {
            escreverLog("Erro na comunicação com cliente: " + e.getMessage());
        }
    }

    private void escreverLog(String texto) {
        Platform.runLater(() -> txtLog.appendText(texto + "\n"));
    }

    private void atualizarBotao(String texto, String cor) {
        Platform.runLater(() -> {
            btnParar.setText(texto);
            btnParar.setStyle("-fx-background-color: " + cor + "; -fx-text-fill: white; -fx-font-size: 14px;");
        });
    }
}
