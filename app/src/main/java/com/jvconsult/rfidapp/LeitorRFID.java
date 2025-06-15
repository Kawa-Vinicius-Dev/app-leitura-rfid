package com.jvconsult.rfidapp;

import android.content.Context;
import android.util.Log;
import com.pda.rfid.IAsynchronousMessage;
import com.pda.rfid.uhf.UHFReader;
import com.port.Adapt;

public class LeitorRFID {
    private boolean aberto = false;

    public LeitorRFID(Context contexto, IAsynchronousMessage callback) {
        Adapt.init(contexto);
        UHFReader.getUHFInstance().OpenConnect(callback);
        aberto = true;
        if (!aberto) {
            Log.d("LeitorRFID", "Falha ao abrir UHF!");
        }
        UHFReader._Config.SetEPCBaseBandParam(255, 0, 1, 0);
        UHFReader._Config.SetANTPowerParam(1, 20);
    }

    public boolean iniciarLeitura() {
        return UHFReader._Tag6C.GetEPC(1, 1) == 0;
    }

    public void pararLeitura() {
        UHFReader.getUHFInstance().Stop();
    }

    public void fechar() {
        UHFReader.getUHFInstance().CloseConnect();
    }
}
