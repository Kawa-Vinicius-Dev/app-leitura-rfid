package com.rktec.rfidapp;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ItemLeituraSessaoAdapter extends RecyclerView.Adapter<ItemLeituraSessaoAdapter.ViewHolder> {

    private List<ItemLeituraSessao> itens;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public ItemLeituraSessaoAdapter(List<ItemLeituraSessao> itens) {
        this.itens = itens;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lido, parent, false);
        return new ViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ItemLeituraSessao sessao = itens.get(position);

        // ---------------- DESCRIÇÃO (sempre descresumida) ----------------
        String descricao;
        if (!sessao.encontrado || sessao.item == null) {
            descricao = "Item não cadastrado";
        } else if (sessao.item.descdetalhada != null && !sessao.item.descdetalhada.trim().isEmpty()) {
            descricao = sessao.item.descdetalhada.trim();
        } else if (sessao.item.descresumida != null && !sessao.item.descresumida.trim().isEmpty()) {
            descricao = sessao.item.descresumida.trim();
        } else {
            descricao = "Item sem descrição";
        }
        holder.tvDescricao.setText(descricao);

        // ---------------- LOJA / SETOR / EPC ----------------
        String loja = "-";
        String setor = "-";

        if (sessao.item != null) {
            if (sessao.item.loja != null && !sessao.item.loja.trim().isEmpty()) {
                loja = sessao.item.loja.trim();
            }
            // usa a coluna CODLOCALIZACAO como "setor"
            if (sessao.item.codlocalizacao != null && !sessao.item.codlocalizacao.trim().isEmpty()) {
                setor = sessao.item.codlocalizacao.trim();
            }
        }

        if (sessao.encontrado && sessao.item != null) {
            String subtexto = "Plaqueta: " + sessao.epc +
                    " | Loja: " + loja +
                    " | Setor: " + setor;
            holder.tvEp.setText(subtexto);
        } else {
            holder.tvEp.setText("EPC: " + sessao.epc);
        }

        // ---------------- ÍCONE E COR POR STATUS ----------------
        int iconResId;
        int corIcone;

        if (!sessao.encontrado || sessao.item == null ||
                sessao.status == ItemLeituraSessao.STATUS_NAO_ENCONTRADO) {

            iconResId = R.drawable.ic_close;
            corIcone = ContextCompat.getColor(
                    holder.itemView.getContext(),
                    R.color.error_red
            );

        } else {
            switch (sessao.status) {
                case ItemLeituraSessao.STATUS_OK:
                    iconResId = R.drawable.ic_check;
                    corIcone = ContextCompat.getColor(
                            holder.itemView.getContext(),
                            R.color.success_green
                    );
                    break;

                case ItemLeituraSessao.STATUS_SETOR_ERRADO:
                    iconResId = R.drawable.ic_check;
                    corIcone = Color.parseColor("#FFC107"); // amarelo
                    break;

                case ItemLeituraSessao.STATUS_LOJA_ERRADA:
                    iconResId = R.drawable.ic_store;
                    corIcone = Color.parseColor("#FB8C00"); // laranja
                    break;

                default:
                    iconResId = R.drawable.ic_check;
                    corIcone = ContextCompat.getColor(
                            holder.itemView.getContext(),
                            R.color.success_green
                    );
                    break;
            }
        }

        holder.iconStatus.setImageResource(iconResId);
        holder.iconStatus.setColorFilter(corIcone);
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDescricao;
        TextView tvEp;
        ImageView iconStatus;

        public ViewHolder(@NonNull View itemView, OnItemClickListener listener) {
            super(itemView);

            tvDescricao = itemView.findViewById(R.id.tvDescricaoItem);
            tvEp = itemView.findViewById(R.id.tvEp);
            iconStatus = itemView.findViewById(R.id.icon_status);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        listener.onItemClick(pos);
                    }
                }
            });
        }
    }
}
