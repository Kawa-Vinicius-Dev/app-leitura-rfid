package com.rktec.rfidapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.util.List;

public class UsuarioAdapter extends ArrayAdapter<Usuario> {

    public interface OnUsuarioRemovidoListener {
        void onRemovido();
    }

    private final Context context;
    private final List<Usuario> usuarios;
    private final UsuarioDAO dao;
    private final OnUsuarioRemovidoListener onRemovido;

    private final String usuarioLogado;
    private final String permissaoLogado;

    public UsuarioAdapter(Context context, List<Usuario> usuarios, UsuarioDAO dao, OnUsuarioRemovidoListener onRemovido) {
        super(context, 0, usuarios);
        this.context = context;
        this.usuarios = usuarios;
        this.dao = dao;
        this.onRemovido = onRemovido;

        usuarioLogado = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
                .getString("usuario_nome", "");
        permissaoLogado = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
                .getString("usuario_permissao", "MEMBRO");
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_usuario, parent, false);
        }

        TextView tvNome = convertView.findViewById(R.id.tvNomeUsuario);
        TextView tvTipo = convertView.findViewById(R.id.tvTipoUsuario);
        ImageButton btnRemover = convertView.findViewById(R.id.btnRemover);

        Usuario usuario = usuarios.get(position);

        String nome = usuario.nome != null ? usuario.nome : "";
        String permissaoUsuario = usuario.permissao != null ? usuario.permissao : "MEMBRO";

        boolean ehCEO = "CEO".equalsIgnoreCase(permissaoUsuario);

        tvNome.setText(nome);

        // Badge por tipo
        if (ehCEO) {
            tvTipo.setText("CEO");
            tvTipo.setBackgroundResource(R.drawable.bg_tipo_usuario_ceo);     // cria esse drawable
        } else if ("ADM".equalsIgnoreCase(permissaoUsuario)) {
            tvTipo.setText("ADM");
            tvTipo.setBackgroundResource(R.drawable.bg_tipo_usuario_adm);     // e esse
        } else {
            tvTipo.setText("MEMBRO");
            tvTipo.setBackgroundResource(R.drawable.bg_tipo_usuario_membro);  // esse já existe
        }

        // CEO e usuário logado não mostram lixeira
        if (ehCEO || nome.equalsIgnoreCase(usuarioLogado)) {
            btnRemover.setVisibility(View.GONE);
        } else {
            btnRemover.setVisibility(View.VISIBLE);
            btnRemover.setOnClickListener(v ->
                    mostrarDialogRemoverUsuario(usuario, position)
            );
        }

        // CEO ou ADM logado podem trocar tipo de quem NÃO é CEO
        boolean podeAlterarTipo =
                ("CEO".equalsIgnoreCase(permissaoLogado) || "ADM".equalsIgnoreCase(permissaoLogado))
                        && !ehCEO;

        if (podeAlterarTipo) {
            View.OnClickListener clickAlterar = v -> mostrarDialogAlterarPermissao(usuario, tvTipo);
            // card todo clicável (root do layout)
            convertView.setOnClickListener(clickAlterar);
            tvTipo.setOnClickListener(clickAlterar);
        } else {
            convertView.setOnClickListener(null);
            tvTipo.setOnClickListener(null);
        }

        return convertView;
    }

    private void mostrarDialogRemoverUsuario(Usuario usuario, int position) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_remover_usuario, null);
        TextView tvMsg = dialogView.findViewById(R.id.tvMsgDialog);
        RadioGroup rgMotivo = dialogView.findViewById(R.id.rgMotivo);
        RadioButton rbDesligamento = dialogView.findViewById(R.id.rbDesligamento);
        RadioButton rbOutros = dialogView.findViewById(R.id.rbOutros);

        tvMsg.setText("Tem certeza que deseja remover o usuário \"" + usuario.nome + "\"?");

        AlertDialog dialog = new AlertDialog.Builder(context, R.style.AppDialogTheme)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        dialogView.findViewById(R.id.btnCancelarDialog).setOnClickListener(x -> dialog.dismiss());

        dialogView.findViewById(R.id.btnRemoverDialog).setOnClickListener(x -> {
            String motivo = rbDesligamento.isChecked()
                    ? "Desligamento de usuário"
                    : "Outros";

            try {
                LogHelper.logRemocaoUsuario(context, usuarioLogado, usuario.nome, motivo);
            } catch (Exception ignored) {}

            dao.removerUsuario(usuario.nome);
            usuarios.remove(position);
            notifyDataSetChanged();
            if (onRemovido != null) onRemovido.onRemovido();

            Toast.makeText(context, "Usuário removido!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void mostrarDialogAlterarPermissao(Usuario usuario, TextView tvTipo) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_alterar_tipo_usuario, null);

        TextView tvNomeUsuarioDialog = view.findViewById(R.id.tvNomeUsuarioDialog);
        RadioGroup rgTipoUsuario = view.findViewById(R.id.rgTipoUsuario);
        RadioButton rbMembro = view.findViewById(R.id.rbMembro);
        RadioButton rbAdm = view.findViewById(R.id.rbAdm);
        View btnCancelar = view.findViewById(R.id.btnCancelarTipo);
        View btnSalvar = view.findViewById(R.id.btnSalvarTipo);

        tvNomeUsuarioDialog.setText("Usuário: " + (usuario.nome != null ? usuario.nome : "-"));

        // Define seleção inicial
        if ("ADM".equalsIgnoreCase(usuario.permissao)) {
            rbAdm.setChecked(true);
        } else {
            rbMembro.setChecked(true);
        }

        AlertDialog dialog = new AlertDialog.Builder(context, R.style.AppDialogTheme)
                .setView(view)
                .setCancelable(true)
                .create();

        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        btnSalvar.setOnClickListener(v -> {
            String novaPermissao;
            int checkedId = rgTipoUsuario.getCheckedRadioButtonId();

            if (checkedId == R.id.rbAdm) {
                novaPermissao = "ADM";
            } else {
                novaPermissao = "MEMBRO";
            }

            // Não mudou nada
            if (novaPermissao.equalsIgnoreCase(usuario.permissao)) {
                dialog.dismiss();
                return;
            }

            boolean ok = dao.atualizarPermissaoUsuario(usuario.nome, novaPermissao);
            if (ok) {
                usuario.permissao = novaPermissao;
                tvTipo.setText("ADM".equalsIgnoreCase(novaPermissao) ? "ADM" : "MEMBRO");
                Toast.makeText(context, "Tipo de usuário atualizado!", Toast.LENGTH_SHORT).show();

                if (onRemovido != null) onRemovido.onRemovido(); // Recarrega lista
            } else {
                Toast.makeText(context, "Erro ao atualizar usuário!", Toast.LENGTH_SHORT).show();
            }

            dialog.dismiss();
        });

        dialog.show();
    }
}
