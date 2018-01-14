package sample;/* Created by Oussama on 13/01/2018. */

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;

import java.io.IOException;

public class ChangeWindows {

    public static ControllerInvite controllerInvite;

    public static void incomingCall(String desc, boolean type){
        setInteface("invite");
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                ChangeWindows.controllerInvite.descCaller.setText(desc);
                ChangeWindows.controllerInvite.type(type);
            }
        });

    }
    public static void goBack(){
        setInteface("home");
    }
    private static void setInteface(String nameFile){
        Platform.runLater(new Runnable() {
            @Override
            public void run() {

                try {

                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource(
                                    nameFile + ".fxml"
                            )
                    );

                    Pane pane = (Pane) loader.load();
                    if(nameFile.equals("invite"))
                        ChangeWindows.controllerInvite =
                            loader.<ControllerInvite>getController();


                    Main.stage.setScene(new Scene(pane, 300, 550));

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
