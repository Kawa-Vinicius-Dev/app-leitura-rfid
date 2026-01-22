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

        // ---------------- DESCRIÇÃO (prioriza descresumida) ----------------
        String descricao;
        if (!sessao.encontrado || sessao.item == null) {
            descricao = "Item não cadastrado";
        } else if (sessao.item.descresumida != null && !sessao.item.descresumida.trim().isEmpty()) {
            descricao = sessao.item.descresumida.trim();
        } else if (sessao.item.descdetalhada != null && !sessao.item.descdetalhada.trim().isEmpty()) {
            descricao = sessao.item.descdetalhada.trim();
        } else {
            descricao = "Item sem descrição";
        }
        holder.tvDescricao.setText(descricao);

        // ---------------- LOJA / SETOR / EPC ----------------
        if (sessao.encontrado && sessao.item != null) {
            String loja = (sessao.item.loja != null) ? sessao.item.loja : "-";
            String setor = (sessao.item.codlocalizacao != null) ? sessao.item.codlocalizacao : "-";

            StringBuilder info = new StringBuilder();
            info.append("Plaqueta: ").append(sessao.epc);

            info.append(" | Loja: ").append(loja);
            info.append(" | Setor: ").append(setor);

// ---- INFORMAÇÕES DE DIVERGÊNCIA ----
            if (sessao.status == ItemLeituraSessao.STATUS_SETOR_ERRADO) {
                if (sessao.setorAntigo != null && !sessao.setorAntigo.isEmpty()) {
                    info.append("\nSetor atual (base): ").append(sessao.setorAntigo);
                }
            }

            if (sessao.status == ItemLeituraSessao.STATUS_LOJA_ERRADA) {
                if (sessao.lojaAntiga != null && !sessao.lojaAntiga.isEmpty()) {
                    info.append("\nLoja atual (base): ").append(sessao.lojaAntiga);
                }
            }

            holder.tvEp.setText(info.toString());

        } else {
            holder.tvEp.setText("EPC: " + sessao.epc);
        }

        // ---------------- ÍCONE E COR POR STATUS ----------------
        int iconResId;
        int corIcone;

        if (!sessao.encontrado || sessao.item == null ||
                sessao.status == ItemLeituraSessao.STATUS_NAO_ENCONTRADO) {

            iconResId = R.drawable.ic_close;
            corIcone = ContextCompat.getColor(holder.itemView.getContext(), R.color.error_red);

        } else {
            switch (sessao.status) {

                case ItemLeituraSessao.STATUS_OK:
                    iconResId = R.drawable.ic_check;
                    corIcone = ContextCompat.getColor(holder.itemView.getContext(), R.color.success_green);
                    break;

                case ItemLeituraSessao.STATUS_SETOR_ERRADO:
                    // mesma loja, setor errado → TRACINHO AMARELO
                    iconResId = R.drawable.ic_dash;
                    corIcone = Color.parseColor("#FFC107");
                    break;

                case ItemLeituraSessao.STATUS_LOJA_ERRADA:
                    // outra loja → LOJINHA AMARELA
                    iconResId = R.drawable.ic_store;
                    corIcone = Color.parseColor("#FFC107");
                    break;

                default:
                    iconResId = R.drawable.ic_check;
                    corIcone = ContextCompat.getColor(holder.itemView.getContext(), R.color.success_green);
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
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onItemClick(getAdapterPosition());
                }
            });
        }
    }
}
