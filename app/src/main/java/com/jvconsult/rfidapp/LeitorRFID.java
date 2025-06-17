package com.jvconsult.rfidapp;

import android.content.Context;
import android.util.Log;
import com.pda.rfid.IAsynchronousMessage;
import com.pda.rfid.uhf.UHFReader;
import com.port.Adapt;

public class LeitorRFID {
    private boolean aberto = false;
    private boolean lendo = false;

    public LeitorRFID(Context contexto, IAsynchronousMessage callback) {
        try {
            Adapt.init(contexto);
            UHFReader.getUHFInstance().OpenConnect(callback);
            aberto = true;
            if (!aberto) {
                Log.d("LeitorRFID", "Falha ao abrir UHF!");
            }
            UHFReader._Config.SetEPCBaseBandParam(255, 0, 1, 0);
            UHFReader._Config.SetANTPowerParam(1, 20);
        } catch (Exception e) {
            Log.e("LeitorRFID", "Erro ao inicializar leitor: " + e.getMessage());
            aberto = false;
        }
    }

    public boolean iniciarLeitura() {
        if (aberto && !lendo) {
            lendo = UHFReader._Tag6C.GetEPC(1, 1) == 0;
            return lendo;
        }
        return false;
    }

    public void pararLeitura() {
        if (aberto && lendo) {
            UHFReader.getUHFInstance().Stop();
            lendo = false;
        }
    }

    public void fechar() {
        if (aberto) {
            UHFReader.getUHFInstance().CloseConnect();
            aberto = false;
            lendo = false;
        }
    }

    public void setPotencia(int potencia) {
        UHFReader._Config.SetANTPowerParam(1, potencia);
    }

}
