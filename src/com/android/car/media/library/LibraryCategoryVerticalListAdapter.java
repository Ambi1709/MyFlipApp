package com.android.car.media;


import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.harman.psa.widget.verticallist.holder.MultiactionViewHolder;
import com.harman.psa.widget.verticallist.holder.PsaViewHolderDataBinder;
import com.harman.psa.widget.verticallist.holder.SectionViewHolder;
import com.harman.psa.widget.verticallist.model.ItemData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class LibraryCategoryVerticalListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final String DEFAULT_SECTION = "DEFAULT";

    private static final int SECTION_TYPE = 0;
    private static final int VIEW_MULTI_ITEM_TYPE = 1;

    public interface OnItemClickListener {
        void onItemClicked(ItemData itemValue);
    }

    private OnItemClickListener mItemClickListener;

    private Map<String, List<ItemData>> mSectionsLists;
    private ArrayList<String> mKeysArray = new ArrayList<>();

    private boolean mShowSubtitles = true;

    public LibraryCategoryVerticalListAdapter(OnItemClickListener listener) {
        mItemClickListener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        RecyclerView.ViewHolder holder;
        switch (viewType) {
            case SECTION_TYPE:
                holder = new SectionViewHolder(viewGroup.getContext());
                break;
            default:
            case VIEW_MULTI_ITEM_TYPE:
                holder = new MultiactionViewHolder(viewGroup.getContext());
                break;
        }
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, final int viewItemPosition) {

        if (viewHolder instanceof PsaViewHolderDataBinder) {
            final ItemData value = (ItemData) getValue(viewItemPosition);
            if (!mShowSubtitles) {
                value.setSecondaryText(null);
            }
            final View checkableView = ((PsaViewHolderDataBinder) viewHolder).bindData(value);

            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mItemClickListener != null) {
                        mItemClickListener.onItemClicked(value);
                    }
                }
            });

        } else if (viewHolder instanceof SectionViewHolder) {
            ((SectionViewHolder) viewHolder).setTitle((String) getValue(viewItemPosition));
        }
    }


    @Override
    public int getItemViewType(int position) {
        int viewType = -1;
        Object dta = getValue(position);
        if (dta instanceof String) {
            viewType = SECTION_TYPE;
        } else {
            viewType = VIEW_MULTI_ITEM_TYPE;
        }

        return viewType;
    }

    private Object getValue(int position) {
        Object obj = null;

        int ind = 0;

        if (mKeysArray.size() == 1) {
            List<ItemData> vals = mSectionsLists.get(mKeysArray.get(0));
            if (ind + vals.size() >= position) {
                obj = vals.get(position - ind);
            }
        } else {
            for (String key : mKeysArray) {
                if (ind == position) {
                    obj = key;
                    break;
                } else {
                    List<ItemData> vals = mSectionsLists.get(key);
                    if (ind + vals.size() >= position) {
                        obj = vals.get(position - ind - 1);
                        break;
                    } else {
                        ind += vals.size();
                    }

                    ind++;
                }
            }
        }

        return obj;
    }

    @Override
    public int getItemCount() {
        return mSectionsLists == null ? 0 : calculateItemsCount(mSectionsLists);
    }

    private int calculateItemsCount(@NonNull Map<String, List<ItemData>> items) {
        int count = items.size();

        if (count == 1) {
            count = 0;
        } // if sections don't use

        for (String key : mKeysArray) {
            count += items.get(key).size();
        }


        return count;
    }


    public void setKeysArray(ArrayList<String> keysArray) {
        this.mKeysArray = keysArray;
    }

    public void setSectionsLists(HashMap<String, List<ItemData>> sectionsLists) {
        this.mSectionsLists = sectionsLists;
    }

    public void hideSubtitles(boolean hideSubtitles) {
        mShowSubtitles = !hideSubtitles;
    }


}