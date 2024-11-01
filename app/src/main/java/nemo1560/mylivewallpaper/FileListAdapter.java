package nemo1560.mylivewallpaper;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.FileViewHolder> {

    private List<String> fileList;
    private OnFileClickListener listener;

    public FileListAdapter(List<String> fileList, OnFileClickListener listener) {
        this.fileList = fileList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        String file = fileList.get(position);
        holder.bind(file);
        holder.itemView.setOnClickListener(v -> listener.onFileClick(file));
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {

        private TextView fileNameTextView;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            fileNameTextView = itemView.findViewById(R.id.fileNameTextView);
        }

        public void bind(String fileName) {
            fileNameTextView.setText(fileName);
        }
    }
}