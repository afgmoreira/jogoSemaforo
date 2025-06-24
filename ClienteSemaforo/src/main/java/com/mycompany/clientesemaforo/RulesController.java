package com.mycompany.clientesemaforo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;

/**
 * Controlador da interface de regras do Cliente do Jogo Semáforo.
 * Responsável por comunicar ao servidor que o jogador está pronto
 * e por aguardar que ambos os jogadores estejam prontos para iniciar o jogo.
 * 
 * Após ambos os jogadores estarem prontos, este controlador muda para o ecrã do tabuleiro (game.fxml).
 * 
 * @author Grupo 07
 * @version 1.0
 * @since 2025-06-16
 */
public class RulesController {

    @FXML
    private Button prontoButton;

    /** Socket de comunicação com o servidor */
    private Socket socket;

    /** Nome do jogador, passado a partir do ConnectionController */
    private String nomeJogador;

    /**
     * Define o nome do jogador atual.
     * 
     * @param nome nome do jogador
     */
    public void setNomeJogador(String nome) {
        this.nomeJogador = nome;
    }

    /**
     * Define o socket que será utilizado para a comunicação com o servidor.
     * 
     * @param socket socket já estabelecido
     */
    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    /**
     * Método chamado ao clicar no botão "PRONTO".
     * Envia ao servidor a mensagem indicando que o jogador está pronto.
     * Depois aguarda a resposta do servidor (COMEÇAR ou ESPERAR) para avançar para o tabuleiro.
     */
    @FXML
    private void switchToPrimary() throws IOException {
        if (socket != null && !socket.isClosed()) {
            new Thread(() -> {
                try {
                    // Enviar "PRONTO" ao servidor
                    DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                    dos.writeUTF("PRONTO");
                    dos.flush();

                    // Atualiza o botão visualmente
                    Platform.runLater(() -> {
                        prontoButton.setText("A ESPERA DO OUTRO JOGADOR");
                        prontoButton.setStyle("-fx-background-color: #6fd96f; -fx-font-weight: bold;");
                        prontoButton.setDisable(true);
                    });

                    // Esperar a resposta do servidor
                    DataInputStream dis = new DataInputStream(socket.getInputStream());
                    String resposta = dis.readUTF();

                    Platform.runLater(() -> {
                        try {
                            // Carrega a interface do jogo
                            FXMLLoader loader = new FXMLLoader(App.class.getResource("game.fxml"));
                            Parent root = loader.load();

                            GameController controller = loader.getController();
                            controller.setSocket(socket);
                            controller.setNomeJogador(nomeJogador);

                            // Define de quem é a vez, conforme resposta do servidor
                            if (resposta.equals("COMEÇAR")) {
                                controller.setMinhaVez(true);
                            } else if (resposta.equals("ESPERAR")) {
                                controller.setMinhaVez(false);
                            }

                            // Avança para o tabuleiro
                            App.getScene().setRoot(root);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}