package com.mycompany.clientesemaforo;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Controlador da interface do tabuleiro do Cliente no Jogo Semáforo.
 * Controla a lógica do jogo entre dois jogadores com comunicação via socket.
 * 
 * Permite jogar alternadamente, exibe o estado do jogo, processa jogadas
 * e controla reinícios ou saídas.
 * 
 * @author Grupo 07
 * @version 1.0
 * @since 2025-06-16
 */
public class GameController {

    @FXML private Label lblEstado;
    @FXML private Label lblVerdes, lblAmarelas, lblVermelhas;
    @FXML private GridPane grid;
    @FXML private Button btnRecomecar, btnSair;

    private Socket socket;
    private DataOutputStream dos;
    private DataInputStream dis;
    private boolean minhaVez;
    private int verdes = 8, amarelas = 8, vermelhas = 8;
    private boolean jogoFinalizado = false;
    private String[][] tabuleiro = new String[3][4];
    private String nomeJogador = "Jogador";
    private String nomeAdversario = "Adversário";

    /**
     * Define o socket de comunicação e inicia a escuta por jogadas.
     * 
     * @param socket socket TCP com o servidor
     */
    public void setSocket(Socket socket) {
        this.socket = socket;
        try {
            this.dos = new DataOutputStream(socket.getOutputStream());
            this.dis = new DataInputStream(socket.getInputStream());
            esperarJogadas();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Inicializa o tabuleiro e elementos visuais.
     */
    @FXML
    public void initialize() {
        criarTabuleiro();
        atualizarContadores();
        lblEstado.setText("A espera da jogada adversária");
        btnRecomecar.setVisible(false);
        btnSair.setVisible(false);
    }

    /**
     * Cria o tabuleiro inicial com células clicáveis.
     */
    private void criarTabuleiro() {
        grid.getChildren().clear();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 4; j++) {
                StackPane cell = new StackPane();
                cell.setPrefSize(80, 80);
                cell.setStyle("-fx-border-color: black; -fx-background-color: white;");
                int row = i;
                int col = j;
                cell.setOnMouseClicked(e -> tratarJogada(cell, row, col));
                grid.add(cell, j, i);
            }
        }
    }

    /**
     * Trata o clique do jogador numa célula do tabuleiro.
     * Executa a jogada e envia ao servidor.
     */
    private void tratarJogada(StackPane cell, int row, int col) {
        if (!minhaVez || jogoFinalizado) return;

        if (cell.getChildren().isEmpty() && verdes > 0) {
            Circle circle = new Circle(25);
            circle.setFill(Color.web("#58d68d"));
            cell.getChildren().add(circle);
            verdes--;
            tabuleiro[row][col] = "verde";
            enviarJogada(row, col, "verde");
            if (verificarVitoria("verde")) {
                fimDeJogo(nomeJogador + " venceu!!");
                try { dos.writeUTF("FIM:" + nomeJogador); } catch (IOException e) {}
                return;
            }
        } else if (!cell.getChildren().isEmpty()) {
            if (cell.getChildren().get(0) instanceof Circle && amarelas > 0) {
                cell.getChildren().clear();
                Polygon triangle = new Polygon(30.0, 0.0, 60.0, 60.0, 0.0, 60.0);
                triangle.setFill(Color.web("#f4d03f"));
                cell.getChildren().add(triangle);
                amarelas--;
                tabuleiro[row][col] = "amarelo";
                enviarJogada(row, col, "amarelo");
                if (verificarVitoria("amarelo")) {
                    fimDeJogo(nomeJogador + " venceu!!");
                    try { dos.writeUTF("FIM:" + nomeJogador); } catch (IOException e) {}
                    return;
                }
            } else if (cell.getChildren().get(0) instanceof Polygon && vermelhas > 0) {
                cell.getChildren().clear();
                Rectangle square = new Rectangle(50, 50);
                square.setFill(Color.web("#e74c3c"));
                cell.getChildren().add(square);
                vermelhas--;
                tabuleiro[row][col] = "vermelho";
                enviarJogada(row, col, "vermelho");
                if (verificarVitoria("vermelho")) {
                    fimDeJogo(nomeJogador + " venceu!!");
                    try { dos.writeUTF("FIM:" + nomeJogador); } catch (IOException e) {}
                    return;
                }
            }
        }

        minhaVez = false;
        atualizarContadores();
        lblEstado.setText("A espera da jogada adversária");
    }

    /**
     * Envia uma jogada ao servidor.
     */
    private void enviarJogada(int row, int col, String tipo) {
        try {
            dos.writeUTF("JOGADA:" + row + ":" + col + ":" + tipo);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Fica à escuta de mensagens recebidas do servidor.
     */
    private void esperarJogadas() {
        new Thread(() -> {
            try {
                while (true) {
                    String msg = dis.readUTF();
                    if (msg.startsWith("JOGADA")) {
                        String[] partes = msg.split(":");
                        int row = Integer.parseInt(partes[1]);
                        int col = Integer.parseInt(partes[2]);
                        String tipo = partes[3];
                        Platform.runLater(() -> aplicarJogadaAdversario(row, col, tipo));
                    } else if (msg.equals("COMEÇAR")) {
                        Platform.runLater(() -> setMinhaVez(true));
                    } else if (msg.equals("ESPERAR")) {
                        Platform.runLater(() -> setMinhaVez(false));
                    } else if (msg.startsWith("FIM:")) {
                        String nomeVencedor = msg.substring(4);
                        Platform.runLater(() -> fimDeJogo(nomeVencedor + " venceu!!"));
                    } else if (msg.equals("SAIU")) {
                        Platform.runLater(() -> fimDeJogo("O adversário saiu do jogo."));
                    } else if (msg.startsWith("NOVO_JOGO")) {
                        String[] partes = msg.split(":");
                        Platform.runLater(() -> {
                            resetarJogo();
                            if (partes.length > 1 && partes[1].equals("COMEÇAR")) {
                                setMinhaVez(true);
                            } else {
                                setMinhaVez(false);
                            }
                        });
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Aplica a jogada recebida do adversário.
     */
    private void aplicarJogadaAdversario(int row, int col, String tipo) {
        StackPane cell = (StackPane) grid.getChildren().get(row * 4 + col);
        cell.getChildren().clear();

        switch (tipo) {
            case "verde":
                cell.getChildren().add(new Circle(25, Color.web("#58d68d")));
                break;
            case "amarelo":
                Polygon triangle = new Polygon(30.0, 0.0, 60.0, 60.0, 0.0, 60.0);
                triangle.setFill(Color.web("#f4d03f"));
                cell.getChildren().add(triangle);
                break;
            case "vermelho":
                Rectangle square = new Rectangle(50, 50);
                square.setFill(Color.web("#e74c3c"));
                cell.getChildren().add(square);
                break;
        }
        tabuleiro[row][col] = tipo;

        if (verificarVitoria(tipo)) {
            fimDeJogo("Vitória do adversário");
            return;
        }

        minhaVez = true;
        lblEstado.setText("É a tua vez!");
    }

    /**
     * Atualiza os contadores de peças visuais.
     */
    private void atualizarContadores() {
        lblVerdes.setText("Peças Verdes: " + verdes);
        lblAmarelas.setText("Peças Amarelas: " + amarelas);
        lblVermelhas.setText("Peças Vermelhas: " + vermelhas);
    }

    /**
     * Finaliza o jogo com mensagem e mostra os botões.
     */
    private void fimDeJogo(String mensagem) {
        jogoFinalizado = true;
        lblEstado.setText(mensagem);
        btnRecomecar.setVisible(true);
        btnSair.setVisible(true);
    }

    /**
     * Envia pedido de reinício ao servidor.
     */
    @FXML
    private void recomecar() {
        try {
            dos.writeUTF("RECOMEÇAR");
            btnRecomecar.setText("À espera do outro jogador");
            btnRecomecar.setDisable(true);
            btnRecomecar.setStyle("-fx-background-color: yellow; -fx-font-weight: bold;");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Fecha o socket e sai da aplicação.
     */
    @FXML
    private void sair() {
        try {
            socket.close();
            Platform.exit();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reinicia o tabuleiro e as variáveis.
     */
    private void resetarJogo() {
        criarTabuleiro();
        verdes = amarelas = vermelhas = 8;
        tabuleiro = new String[3][4];
        atualizarContadores();
        lblEstado.setText("A tua vez ou espera do adversário");
        btnRecomecar.setVisible(false);
        btnSair.setVisible(false);
        btnRecomecar.setDisable(false);
        btnRecomecar.setText("RECOMEÇAR");
        btnRecomecar.setStyle("-fx-background-color: #58d68d; -fx-font-weight: bold; -fx-text-fill: white; -fx-border-color: black; -fx-border-width: 2;");
        jogoFinalizado = false;
    }

    /**
     * Define se é a vez do jogador e atualiza o estado.
     */
    public void setMinhaVez(boolean minhaVez) {
        this.minhaVez = minhaVez;
        if (minhaVez) {
            lblEstado.setText("É a tua vez!");
        } else {
            lblEstado.setText("A espera da jogada adversária");
        }
    }

    /**
     * Define o nome do jogador local.
     * @param nome nome do jogador
     */
    public void setNomeJogador(String nome) {
        this.nomeJogador = nome;
    }

    /**
     * Verifica se há 3 peças da mesma cor em linha, coluna ou diagonal.
     * @param tipo cor da peça ("verde", "amarelo", "vermelho")
     * @return true se houve vitória
     */
    private boolean verificarVitoria(String tipo) {
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 2; j++)
                if (tipo.equals(tabuleiro[i][j]) && tipo.equals(tabuleiro[i][j+1]) && tipo.equals(tabuleiro[i][j+2]))
                    return true;

        for (int j = 0; j < 4; j++)
            for (int i = 0; i < 1; i++)
                if (tipo.equals(tabuleiro[i][j]) && tipo.equals(tabuleiro[i+1][j]) && tipo.equals(tabuleiro[i+2][j]))
                    return true;

        for (int i = 0; i < 1; i++)
            for (int j = 0; j < 2; j++)
                if (tipo.equals(tabuleiro[i][j]) && tipo.equals(tabuleiro[i+1][j+1]) && tipo.equals(tabuleiro[i+2][j+2]))
                    return true;

        for (int i = 0; i < 1; i++)
            for (int j = 2; j < 4; j++)
                if (tipo.equals(tabuleiro[i][j]) && tipo.equals(tabuleiro[i+1][j-1]) && tipo.equals(tabuleiro[i+2][j-2]))
                    return true;

        return false;
    }
}
