package org.gtdev.tridomhcapture.viewmodel;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.Bindable;
import androidx.databinding.BindingAdapter;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.Observable;
import androidx.lifecycle.ViewModel;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.widget.RecyclerView;

import org.gtdev.tridomhcapture.R;
import org.gtdev.tridomhcapture.databinding.FilelistItemBinding;
import org.gtdev.tridomhcapture.ui.FileListFragment;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PlaybackListViewModel extends ViewModel implements Observable {

    private FileListAdapter adapter;
    private List<RSFile> data;
    private File folder;

    public String message;

    public PlaybackListViewModel() {
        adapter = new FileListAdapter();
        data = new ArrayList<>();
    }

    public void init(FileListFragment parentFrag) {
        adapter.setParentFrag(parentFrag);
        folder = new File(parentFrag.getContext().getExternalFilesDir(null).getAbsolutePath() +
                File.separator + "video");
//        for(int i=0;i<10;i++)
//            data.add(new RSFile("test.bin", folder, ""));
        if(!folder.exists()) {
            message = parentFrag.getString(R.string.empty_file_message);
            return;
        }
        final File[] files = folder.listFiles();
        if(files.length == 0) {
            message = parentFrag.getString(R.string.empty_file_message);
            return;
        }
        message = parentFrag.getString(R.string.select_file_message);
        for(File f:files)
            data.add(new RSFile(f));
    }

    @Override
    public void addOnPropertyChangedCallback(OnPropertyChangedCallback callback) {

    }

    @Override
    public void removeOnPropertyChangedCallback(OnPropertyChangedCallback callback) {

    }

    public static class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.DataViewHolder> {
        private List<RSFile> data = new ArrayList<>();
        private FileListFragment parentFrag;
        private SelectionTracker mSelectionTracker;

        public void setParentFrag(FileListFragment parentFrag) {
            this.parentFrag = parentFrag;
        }

        public void updateData(@Nullable List<RSFile> data) {
            this.data = data;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public DataViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.filelist_item,
                    new FrameLayout(parent.getContext()), false);

            return new DataViewHolder(itemView, data);
        }

        @Override
        public void onBindViewHolder(@NonNull DataViewHolder holder, int position) {
            RSFile fileModel = data.get(position);
            FileListItemViewModel fileListItemViewModel = new FileListItemViewModel(fileModel, parentFrag);
            holder.setViewModel(fileListItemViewModel);
            if (mSelectionTracker != null) {
                if(mSelectionTracker.isSelected(fileModel.file.getPath())){
                    fileListItemViewModel.showSelection = true;
                }
            }
        }

        @Override
        public int getItemCount() {
            return this.data.size();
        }

        @Override
        public void onViewAttachedToWindow(@NotNull DataViewHolder holder) {
            super.onViewAttachedToWindow(holder);
            holder.bind();
        }

        @Override
        public void onViewDetachedFromWindow(@NotNull DataViewHolder holder) {
            super.onViewDetachedFromWindow(holder);
            holder.unbind();
        }

        public void setSelectionTracker(SelectionTracker mSelectionTracker) {
            this.mSelectionTracker = mSelectionTracker;
        }

        public static class DataViewHolder extends RecyclerView.ViewHolder {
            FilelistItemBinding binding;
            private List<RSFile> data;

            public DataViewHolder(@NonNull View itemView, List<RSFile> data) {
                super(itemView);
                this.data = data;
                bind();
            }

            void bind() {
                if (binding == null) {
                    binding = DataBindingUtil.bind(itemView);
                    binding.executePendingBindings();
                }
            }

            void unbind() {
                if (binding != null) {
                    binding.unbind();
                }
            }

            void setViewModel(FileListItemViewModel viewModel) {
                if (binding != null) {
                    binding.setViewModel(viewModel);
                }
            }

            public ItemDetailsLookup.ItemDetails getItemDetails() {
                return new ItemDetailsLookup.ItemDetails() {
                    @Override
                    public int getPosition() {
                        return getAdapterPosition();
                    }

                    @Nullable
                    @Override
                    public Object getSelectionKey() {
                        return data.get(getAdapterPosition()).file.getPath();
                    }
                };
            }
        }

    }

    public static class RSFile {
        String title;
        File file;
        RSFile(String title, File folder, String path) {
            this.title = title;
            if (path!=null)
                file = new File(folder, path);
        }
        RSFile(File f) {
            file = f;
            title = f.getName();
        }

        public File getFile() {
            return file;
        }

        public String getTitle() {
            return title;
        }
    }

    public RSFile getRSFileByPath(String path) {
        for(RSFile rsFile:data) {
            if (rsFile.file.getPath()==path)
                return rsFile;
        }
        return null;
    }

    @Bindable
    public List<RSFile> getData() {
        return this.data;
    }

    @Bindable
    public FileListAdapter getAdapter() {
        return this.adapter;
    }

    @BindingAdapter({"adapter", "data"})
    public static void bind(RecyclerView recyclerView, FileListAdapter adapter, List<RSFile> data) {
        recyclerView.setAdapter(adapter);
        adapter.updateData(data);
    }

}
