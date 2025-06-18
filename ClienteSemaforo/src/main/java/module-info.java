module com.mycompany.clientesemaforo {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.mycompany.clientesemaforo to javafx.fxml;
    exports com.mycompany.clientesemaforo;
}
