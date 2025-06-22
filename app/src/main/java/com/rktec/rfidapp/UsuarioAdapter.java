package com.rktec.rfidapp;

import android.content.Context;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;

import com.rktec.rfidapp.R;

import java.util.List;

public class UsuarioAdapter extends ArrayAdapter<com.rktec.rfidapp.Usuario> {

    private final Context context;
    private final List<com.rktec.rfidapp.Usuario> usuarios;
    private final com.rktec.rfidapp.UsuarioDAO dao;
    private final OnUsuarioRemovidoListener onRemovido;
    private final String usuarioLogado;

    public interface OnUsuarioRemovidoListener {
        void onRemovido();
    }

    public UsuarioAdapter(Context context, List<com.rktec.rfidapp.Usuario> usuarios, com.rktec.rfidapp.UsuarioDAO dao, OnUsuarioRemovidoListener onRemovido) {
        super(context, 0, usuarios);
        this.context = context;
        this.usuarios = usuarios;
        this.dao = dao;
        this.onRemovido = onRemovido;
        // Recupera o usuário logado uma vez só
        this.usuarioLogado = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
                .getString("usuario_nome", "");
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        com.rktec.rfidapp.Usuario usuario = usuarios.get(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_usuario, parent, false);
        }

        TextView tvNome = convertView.findViewById(R.id.tvNomeUsuario);
        TextView tvTipo = convertView.findViewById(R.id.tvTipoUsuario);
        ImageButton btnRemover = convertView.findViewById(R.id.btnRemover);

        tvNome.setText(usuario.nome);

        // ADM ou MEMBRO
        tvTipo.setText(usuario.permissao.equalsIgnoreCase("ADM") ? "ADM" : "MEMBRO");
        // Altera cor de fundo conforme permissão
        if (usuario.permissao.equalsIgnoreCase("ADM")) {
            tvTipo.setBackgroundResource(R.drawable.bg_tipo_usuario_membro); // Azul
        } else {
            tvTipo.setBackgroundResource(R.drawable.bg_tipo_usuario_membro); // Laranja, se quiser pode separar o bg.
        }

        // --- NUNCA deixar o logado se autoremover! ---
        if (usuario.nome.equalsIgnoreCase(usuarioLogado)) {
            btnRemover.setVisibility(View.GONE); // Esconde o botão se for ele mesmo
        } else {
            btnRemover.setVisibility(View.VISIBLE);

            btnRemover.setOnClickListener(v -> {
                View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_remover_usuario, null);

                TextView tvMsg = dialogView.findViewById(R.id.tvMsgDialog);
                tvMsg.setText("Remover o usuário '" + usuario.nome + "'?");

                RadioGroup rgMotivo = dialogView.findViewById(R.id.rgMotivo);
                RadioButton rbDesligamento = dialogView.findViewById(R.id.rbDesligamento);
                RadioButton rbOutros = dialogView.findViewById(R.id.rbOutros);

                AlertDialog dialog = new AlertDialog.Builder(context)
                        .setView(dialogView)
                        .setCancelable(false)
                        .create();

                dialogView.findViewById(R.id.btnCancelarDialog).setOnClickListener(x -> dialog.dismiss());

                dialogView.findViewById(R.id.btnRemoverDialog).setOnClickListener(x -> {
                    // Motivo selecionado
                    String motivo = rbDesligamento.isChecked() ? "Desligamento de usuário" : "Outros";

                    // Faz log
                    com.rktec.rfidapp.LogHelper.logRemocaoUsuario(context, usuarioLogado, usuario.nome, motivo);

                    // Remove usuário no banco e na lista
                    dao.removerUsuario(usuario.nome);
                    usuarios.remove(position);
                    notifyDataSetChanged();
                    if (onRemovido != null) onRemovido.onRemovido();

                    Toast.makeText(context, "Usuário removido!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });

                dialog.show();
            });
        }

        return convertView;
    }
}
