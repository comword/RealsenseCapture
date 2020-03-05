package org.gtdev.tridomhcapture.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.selection.SelectionPredicates;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import org.gtdev.tridomhcapture.R;
import org.gtdev.tridomhcapture.databinding.FilelistFragBinding;
import org.gtdev.tridomhcapture.viewmodel.PlaybackListViewModel;

import java.util.ArrayList;
import java.util.List;

public class FileListFragment extends Fragment {

    private PlaybackListViewModel mViewModel = null;
    private FilelistFragBinding binding;

    public static FileListFragment newInstance() {
        return new FileListFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        boolean initRecycler = false;
        if(mViewModel==null) {
            mViewModel = ViewModelProviders.of(getActivity()).get(PlaybackListViewModel.class);
            mViewModel.init(this);
            initRecycler = true;
        }
        if(binding==null)
            binding = DataBindingUtil.inflate(inflater, R.layout.filelist_frag, container, false);
        View rootView = binding.getRoot();
        binding.setLifecycleOwner(this);
        binding.setViewModel(mViewModel);
        Toolbar toolbar = rootView.findViewById(R.id.toolbar);
        AppCompatActivity aca = ((AppCompatActivity)getActivity());
        aca.setSupportActionBar(toolbar);
        aca.setTitle(aca.getResources().getString(R.string.title_file_list));
        if(initRecycler) {
            RecyclerView recyclerView = rootView.findViewById(R.id.file_list);
            recyclerView.setAdapter(mViewModel.getAdapter());
            recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
            recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), LinearLayoutManager.VERTICAL));
            List<String> items = new ArrayList<>();
            for(PlaybackListViewModel.RSFile f:mViewModel.getData())
                items.add(f.getFile().getPath());
            SelectionTracker mSelectionTracker = new SelectionTracker.Builder<String>(
                    "FileItemSelection",
                    recyclerView,
                    new StringItemKeyProvider(1, items),
                    new StringItemDetailsLookup(recyclerView),
                    StorageStrategy.createStringStorage())
                    .withSelectionPredicate(new StringItemPredicate())
                    .build();
            mViewModel.getAdapter().setSelectionTracker(mSelectionTracker);
        }
        return rootView;
    }

//    @Override
//    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
//        super.onActivityCreated(savedInstanceState);
//        mViewModel = ViewModelProviders.of(this).get(PlaybackListViewModel.class);
//    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.filelist_frag_menu, menu);
        super.onCreateOptionsMenu(menu,inflater);
    }

    static class StringItemKeyProvider extends ItemKeyProvider<String> {
        private List<String> items;
        public StringItemKeyProvider(int scope, List<String> items) {
            super(scope);
            this.items = items;
        }

        @Nullable
        @Override
        public String getKey(int position) {
            return items.get(position);
        }

        @Override
        public int getPosition(@NonNull String key) {
            return items.indexOf(key);
        }
    }

    static class StringItemDetailsLookup extends ItemDetailsLookup {

        private final RecyclerView mRecyclerView;

        StringItemDetailsLookup(RecyclerView recyclerView) {
            mRecyclerView = recyclerView;
        }

        @Nullable
        @Override
        public ItemDetails getItemDetails(@NonNull MotionEvent e) {
            View view = mRecyclerView.findChildViewUnder(e.getX(), e.getY());
            if (view != null) {
                RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(view);
                if (holder instanceof PlaybackListViewModel.FileListAdapter.DataViewHolder) {
                    return ((PlaybackListViewModel.FileListAdapter.DataViewHolder) holder).getItemDetails();
                }
            }
            return null;
        }
    }

    static class StringItemPredicate extends SelectionTracker.SelectionPredicate<String> {

        @Override
        public boolean canSetStateForKey(@NonNull String key, boolean nextState) {
            return false;
        }

        @Override
        public boolean canSetStateAtPosition(int position, boolean nextState) {
            return false;
        }

        @Override
        public boolean canSelectMultiple() {
            return true;
        }
    }

}
