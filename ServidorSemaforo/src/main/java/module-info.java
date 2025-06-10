module com.mycompany.servidorsemaforo {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.mycompany.servidorsemaforo to javafx.fxml;
    exports com.mycompany.servidorsemaforo;
}
