package com.mycompany.clientesemaforo;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;

/**
 * Controlador da interface de conexão do Cliente do Jogo Semáforo.
 * Responsável por recolher os dados de IP, Porta e Nome do jogador,
 * estabelecer a ligação ao servidor e encaminhar para o ecrã de regras.
 *
 * @author Grupo 07
 * @version 1.0
 * @since 2025-06-16
 */
public class ConnectionController {

    @FXML private TextField txtIp;
    @FXML private TextField txtPorta;
    @FXML private TextField txtNome;

    /** Socket usado para comunicar com o servidor */
    private Socket socket;

    /**
     * Método chamado ao clicar no botão "Conectar".
     * Valida os campos, estabelece ligação ao servidor e avança para o ecrã de regras.
     */
    @FXML
    private void conectar() {
        String ip = txtIp.getText().trim();
        String portaStr = txtPorta.getText().trim();
        String nome = txtNome.getText().trim();

        if (ip.isEmpty() || portaStr.isEmpty() || nome.isEmpty()) {
            mostrarAlerta("Todos os campos devem ser preenchidos.");
            return;
        }

        int porta;
        try {
            porta = Integer.parseInt(portaStr);
        } catch (NumberFormatException e) {
            mostrarAlerta("Porta inválida.");
            return;
        }

        // Conectar em background para não bloquear a UI
        new Thread(() -> {
            try {
                socket = new Socket(ip, porta);

                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                dos.writeUTF("Jogador " + nome + " conectado!");

                Platform.runLater(() -> {
                    try {
                        FXMLLoader loader = new FXMLLoader(App.class.getResource("rules.fxml"));
                        Parent root = loader.load();
                        RulesController controller = loader.getController();
                        controller.setSocket(socket);
                        controller.setNomeJogador(nome);  // Passa o nome do jogador ao próximo controlador
                        App.getScene().setRoot(root);
                    } catch (IOException e) {
                        mostrarAlerta("Erro ao mudar de ecrã: " + e.getMessage());
                    }
                });

                // O socket permanece aberto para uso posterior (ex: no ecrã de regras e jogo)

            } catch (IOException e) {
                Platform.runLater(() ->
                    mostrarAlerta("Erro ao conectar ao servidor:\n" + e.getMessage())
                );
            }
        }).start();
    }

    /**
     * Mostra uma janela de alerta com a mensagem de erro fornecida.
     *
     * @param mensagem texto a exibir na janela de alerta
     */
    private void mostrarAlerta(String mensagem) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro");
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}
