package com.example.practica2.ServicioMusica;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

public class ReceiverTelefono extends BroadcastReceiver {
    TelephonyManager telManager;
    ServicioMusica elservicio;

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.

        telManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        telManager.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE);
        System.out.println("RECIEVER TELEFONO");
        ServicioMusica.miBinder binder = (ServicioMusica.miBinder)peekService(context, new Intent(context, ServicioMusica.class)) ;
        if(binder!=null){
            elservicio = binder.obtenServicio();
        }


    }

    private final PhoneStateListener phoneListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            try {
                switch (state) {
                    case TelephonyManager.CALL_STATE_RINGING: {
                        System.out.println("ENTRA AQUI");
                        //Teléfono sonando
                        elservicio.pararMusica();
                        break;
                    }
                    case TelephonyManager.CALL_STATE_OFFHOOK: {
                        //Teléfono descolgado
                        elservicio.pararMusica();
                        break;
                    }
                    case TelephonyManager.CALL_STATE_IDLE: {
                        //Teléfono inactivo
                        elservicio.resume();
                        break;
                    }
                    default: { }
                }
            } catch (Exception ex) {

            }
        }
    };
}

