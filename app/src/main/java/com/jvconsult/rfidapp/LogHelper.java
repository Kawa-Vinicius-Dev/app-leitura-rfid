package com.jvconsult.rfidapp;

import android.content.Context;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class LogHelper {

    // Busca o nome do setor por código
    private static String buscarNomeSetorPorCodigo(String codlocalizacao, List<SetorLocalizacao> listaSetores) {
        if (codlocalizacao == null || listaSetores == null) return "";
        for (SetorLocalizacao setor : listaSetores) {
            if (codlocalizacao.equals(setor.codlocalizacao)) {
                return setor.setor;
            }
        }
        return codlocalizacao; // fallback: se não achar, mostra o código
    }

    // Relatório consolidado CSV por loja
    public static void logRelatorioPorLoja(
            Context context,
            String usuario,
            String loja,
            String setorCodigo,
            List<ItemPlanilha> itensMovidos,
            List<ItemPlanilha> itensOutrasLojas,
            List<String> epcsNaoCadastrados
    ) {
        try {
            // Recupera lista de setores
            List<SetorLocalizacao> listaSetores = DadosGlobais.getInstance().getListaSetores();

            // Nome do setor por extenso
            String setorNome = buscarNomeSetorPorCodigo(setorCodigo, listaSetores);

            File pastaLoja = new File(context.getExternalFilesDir(null), loja + "_RELAT");
            if (!pastaLoja.exists()) pastaLoja.mkdirs();

            File arquivo = new File(pastaLoja, loja + "_RELAT.csv");
            boolean novoArquivo = !arquivo.exists();

            FileWriter writer = new FileWriter(arquivo, true); // Append
            PrintWriter pw = new PrintWriter(writer);

            // Cabeçalho só se for novo arquivo!
            if (novoArquivo) {
                pw.println("Data/Hora,Usuário,Loja,Setor,Tipo,Desc. Item,Plaqueta,Cód. Localização,Alterações");
            }
            String data = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

            for (ItemPlanilha item : itensMovidos) {
                String setorItemNome = buscarNomeSetorPorCodigo(item.codlocalizacao, listaSetores);
                pw.printf("%s,%s,%s,%s,MOVIDO,%s,%s,%s,%n",
                        data, usuario, loja, setorItemNome, item.descresumida, item.nroplaqueta, item.codlocalizacao);
            }
            for (ItemPlanilha item : itensOutrasLojas) {
                String setorItemNome = buscarNomeSetorPorCodigo(item.codlocalizacao, listaSetores);
                pw.printf("%s,%s,%s,%s,OUTRA LOJA/SETOR,%s,%s,%s,%n",
                        data, usuario, item.loja, setorItemNome, item.descresumida, item.nroplaqueta, item.codlocalizacao);
            }
            for (String epc : epcsNaoCadastrados) {
                pw.printf("%s,%s,%s,%s,NAO CADASTRADO,EPC,%s,,%n",
                        data, usuario, loja, setorNome, epc);
            }

            pw.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Loga cada edição feita pelo usuário no relatório da loja
    public static void logEdicaoItem(
            Context context,
            String usuario,
            String loja,
            String setorCodigo,
            ItemPlanilha itemAntigo,
            ItemPlanilha itemNovo,
            String camposAlterados // String descrevendo o que mudou
    ) {
        try {
            List<SetorLocalizacao> listaSetores = DadosGlobais.getInstance().getListaSetores();
            String setorNome = buscarNomeSetorPorCodigo(setorCodigo, listaSetores);

            File pastaLoja = new File(context.getExternalFilesDir(null), loja + "_RELAT");
            if (!pastaLoja.exists()) pastaLoja.mkdirs();

            File arquivo = new File(pastaLoja, loja + "_RELAT.csv");
            boolean novoArquivo = !arquivo.exists();

            FileWriter writer = new FileWriter(arquivo, true); // Append
            PrintWriter pw = new PrintWriter(writer);

            // Cabeçalho só se for novo arquivo!
            if (novoArquivo) {
                pw.println("Data/Hora,Usuário,Loja,Setor,Tipo,Desc. Item,Plaqueta,Cód. Localização,Alterações");
            }
            String data = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

            pw.printf("%s,%s,%s,%s,EDICAO,%s,%s,%s,%s%n",
                    data, usuario, loja, setorNome,
                    itemNovo != null ? itemNovo.descresumida : "",
                    itemNovo != null ? itemNovo.nroplaqueta : "",
                    itemNovo != null ? itemNovo.codlocalizacao : "",
                    camposAlterados != null ? camposAlterados : ""
            );

            pw.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
