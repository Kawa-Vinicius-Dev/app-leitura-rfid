package com.rktec.rfidapp;

import android.content.Context;
import android.util.Log;

import com.pda.rfid.IAsynchronousMessage;
import com.pda.rfid.uhf.UHFReader;
import com.port.Adapt;

public class LeitorRFID {

    private boolean aberto = false;
    private volatile boolean lendo = false;

    private Thread threadLeitura;

    // Antena (normalmente 1)
    private static final int ANTENA = 1;

    // Valores típicos de potência em muitos handhelds: 5..33 (varia por modelo)
    private static final int POT_MIN = 5;
    private static final int POT_MAX = 33;

    public LeitorRFID(Context contexto, IAsynchronousMessage callback) {
        try {
            Adapt.init(contexto);

            // Alguns SDKs retornam int, outros void. Se o seu retornar int, dá pra validar melhor.
            // Aqui mantemos compatível.
            UHFReader.getUHFInstance().OpenConnect(callback);
            aberto = true;

            // Mantive seu baseband (não mexi na sua escolha)
            UHFReader._Config.SetEPCBaseBandParam(255, 0, 1, 0);

            // Não comece “travado” em 20 (médio). Comece num padrão melhor.
            // Se seu slider depois setar, ele vai sobrescrever.
            UHFReader._Config.SetANTPowerParam(ANTENA, 30);

        } catch (Exception e) {
            Log.e("LeitorRFID", "Erro ao inicializar leitor: " + e.getMessage(), e);
            aberto = false;
        }
    }

    public boolean iniciarLeitura() {
        if (!aberto || lendo) return false;

        lendo = true;

        // Leitura contínua: tenta várias vezes por segundo enquanto "lendo" estiver true.
        threadLeitura = new Thread(() -> {
            try {
                while (lendo) {
                    // Chamada que aciona o callback (OutPutEPC) no seu SDK.
                    // Mantemos o seu método, só repetindo em loop.
                    UHFReader._Tag6C.GetEPC(ANTENA, 1);

                    // Pequena pausa para não travar CPU (ajuste 20~80ms se quiser)
                    try { Thread.sleep(30); } catch (InterruptedException ignored) {}
                }
            } catch (Exception e) {
                Log.e("LeitorRFID", "Erro durante leitura contínua: " + e.getMessage(), e);
            }
        }, "RFID-ReadLoop");

        threadLeitura.start();
        return true;
    }

    public void pararLeitura() {
        if (!aberto) return;

        lendo = false;

        try {
            UHFReader.getUHFInstance().Stop();
        } catch (Exception e) {
            Log.e("LeitorRFID", "Erro ao parar leitura: " + e.getMessage(), e);
        }
    }

    public void fechar() {
        try {
            pararLeitura();
            if (aberto) {
                UHFReader.getUHFInstance().CloseConnect();
            }
        } catch (Exception e) {
            Log.e("LeitorRFID", "Erro ao fechar leitor: " + e.getMessage(), e);
        } finally {
            aberto = false;
            lendo = false;
        }
    }

    public void setPotencia(int potencia) {
        if (!aberto) return;

        // Evita potência 0/1/2 (que derruba muito o alcance)
        int p = potencia;
        if (p < POT_MIN) p = POT_MIN;
        if (p > POT_MAX) p = POT_MAX;

        try {
            UHFReader._Config.SetANTPowerParam(ANTENA, p);
        } catch (Exception e) {
            Log.e("LeitorRFID", "Erro ao setar potência: " + e.getMessage(), e);
        }
    }
}
