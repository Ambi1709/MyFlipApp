package com.android.car.media;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;

import com.harman.psa.widget.dropdowns.DropdownDialog;
import com.harman.psa.widget.dropdowns.DropdownHelper;
import com.harman.psa.widget.dropdowns.DropdownItem;
import com.harman.psa.widget.dropdowns.listener.OnDropdownItemClickListener;
import com.harman.psa.widget.verticallist.holder.MultiactionViewHolder;
import com.harman.psa.widget.verticallist.holder.PsaViewHolderDataBinder;
import com.harman.psa.widget.verticallist.holder.SectionViewHolder;
import com.harman.psa.widget.verticallist.holder.SimpleActionViewHolder;
import com.harman.psa.widget.verticallist.items.BaseItemView;
import com.harman.psa.widget.verticallist.items.MultiActionItemView;
import com.harman.psa.widget.verticallist.model.ItemData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class LibraryCategoryVerticalListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "LibraryCategoryVerticalListAdapter";

    public static final String DEFAULT_SECTION = "DEFAULT";

    private static final int SECTION_TYPE = 0;
    private static final int VIEW_MULTI_ITEM_TYPE = 1;
    private static final int VIEW_SIMPLE_ITEM_TYPE = 2;

    public interface OnItemClickListener {
        void onItemClicked(ItemData itemValue);

        void onActionClicked(ItemData itemValue, int action);

        void onGeneralActionClicked(ItemData itemValue);
    }

    private OnItemClickListener mItemClickListener;

    private DropdownDialog mDropdownDialog;

    private Map<String, List<ItemData>> mSectionsLists;
    private ArrayList<String> mKeysArray = new ArrayList<>();

    private boolean mShowSubtitles = true;

    private boolean mIsEditMode;

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
            case VIEW_SIMPLE_ITEM_TYPE:
                holder = new SimpleActionViewHolder(viewGroup.getContext());
                break;
            case VIEW_MULTI_ITEM_TYPE:
            default:
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

            if (viewHolder instanceof MultiactionViewHolder) {
                ((MultiActionItemView) (viewHolder.itemView)).setAction2ClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!mIsEditMode) {
                            showDropdownDialog(view, value);
                        } else {
                            mItemClickListener.onItemClicked(value);
                        }
                    }
                });
            }
            if (viewHolder instanceof SimpleActionViewHolder) { //shuffle play category action
                viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mItemClickListener != null) {
                            mItemClickListener.onGeneralActionClicked(value);
                        }
                    }
                });
            } else {
                viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mItemClickListener != null) {
                            mItemClickListener.onItemClicked(value);
                        }
                    }
                });
            }

            if (mIsEditMode) {
                viewHolder.itemView.setBackgroundResource(R.drawable.psa_general_generic_state_container_focus);
                ((BaseItemView)(viewHolder.itemView)).getPrimaryTextView().setTextColor(viewHolder.itemView.getContext().getResources().getColor(R.color.psa_default_grid_list_item_state_text_color_focused));
                ((BaseItemView)(viewHolder.itemView)).getSecondaryTextView().setTextColor(viewHolder.itemView.getContext().getResources().getColor(R.color.psa_default_grid_list_item_state_text_color_focused));
            } else {
                viewHolder.itemView.setBackgroundResource(R.drawable.psa_general_generic_state_container);
                ((BaseItemView)(viewHolder.itemView)).getPrimaryTextView().setTextColor(viewHolder.itemView.getContext().getResources().getColorStateList(R.color.psa_default_grid_list_item_state_text_color));
                ((BaseItemView)(viewHolder.itemView)).getSecondaryTextView().setTextColor(viewHolder.itemView.getContext().getResources().getColorStateList(R.color.psa_default_grid_list_item_state_text_color));
            }

        } else if (viewHolder instanceof SectionViewHolder) {
            ((SectionViewHolder) viewHolder).setTitle((String) getValue(viewItemPosition));
        }

    }

    private void showDropdownDialog(View view, ItemData itemData) {
        Context context = view.getContext();

        Bundle itemExtras = itemData.getExtras();
        if (itemExtras != null) {
            mDropdownDialog = new DropdownDialog(context, DropdownDialog.VERTICAL);

            if (itemExtras.getBoolean(MediaLibraryController.PLAY_ITEM_ACTION_KEY, false) == true) {
                String playItemText = itemExtras.getString(MediaLibraryController.PLAY_ITEM_ACTION_TEXT_KEY,
                        context.getResources().getString(R.string.library_item_play));
                mDropdownDialog.addDropdownItem(
                        new DropdownItem(MediaLibraryController.PLAY_ITEM_ACTION_INDEX,
                                playItemText,
                                R.drawable.psa_media_button_icon_play));

                if (itemExtras.getBoolean(MediaLibraryController.ADD_TOP_ACTION_KEY, false) == true) {
                    mDropdownDialog.addDropdownItem(
                            new DropdownItem(MediaLibraryController.ADD_TOP_ACTION_INDEX,
                                    context.getResources().getString(R.string.library_item_play_next),
                                    R.drawable.psa_media_library_item_action_add_top_icon));
                }
                if (itemExtras.getBoolean(MediaLibraryController.ADD_BOTTOM_ACTION_KEY, false) == true) {
                    mDropdownDialog.addDropdownItem(
                            new DropdownItem(MediaLibraryController.ADD_BOTTOM_ACTION_INDEX,
                                    context.getResources().getString(R.string.library_item_play_later),
                                    R.drawable.psa_media_library_item_action_add_bottom_icon));
                }
                if (itemExtras.getBoolean(MediaLibraryController.PLAY_SHUFFLE_ACTION_KEY, false) == true) {
                    mDropdownDialog.addDropdownItem(
                            new DropdownItem(MediaLibraryController.PLAY_SHUFFLE_ACTION_INDEX,
                                    context.getResources().getString(R.string.library_item_play_shuffle),
                                    R.drawable.psa_media_button_icon_shuffle_on));
                }

                //Set listener for action item clicked
                mDropdownDialog.setOnActionItemClickListener(new OnDropdownItemClickListener() {
                    @Override
                    public void onItemClick(DropdownItem item) {
                        mItemClickListener.onActionClicked(itemData, item.getItemId());
                    }
                });

                mDropdownDialog.show(view, DropdownHelper.Side.RIGHT);
            }
        }
    }

    public void dismissDialog() {
        if (mDropdownDialog != null) {
            mDropdownDialog.dismiss();
        }
    }


    @Override
    public int getItemViewType(int position) {
        int viewType = -1;
        Object dta = getValue(position);
        if (dta instanceof String) {
            viewType = SECTION_TYPE;
        } else {
            ItemData data = (ItemData) dta;
            if ((data.getAction2ResId() == -1 && data.getAction3ResId() == -1)) {
                viewType = VIEW_SIMPLE_ITEM_TYPE;
            } else {
                viewType = VIEW_MULTI_ITEM_TYPE;
            }
        }
        return viewType;
    }

    private Object getValue(int position) {
        Object obj = null;

        int ind = 0;

        if (mKeysArray.size() == 1) {
            List<ItemData> vals = mSectionsLists.get(mKeysArray.get(0));
            if (vals.size() >= position) {
                obj = vals.get(position);
            }
        } else {
            for (String key : mKeysArray) {
                if (ind == position && !key.equals(DEFAULT_SECTION)) {
                    obj = key;
                    break;
                } else {
                    if (key.equals(DEFAULT_SECTION)) { //it's unconsidered
                        --ind;
                    }
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
            if (key.equals(DEFAULT_SECTION) && count != 0) { //it's unconsidered
                --count;
            }
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

    public void setEditModeEnabled(boolean isEditModeEnabled) {
        mIsEditMode = isEditModeEnabled;
        notifyDataSetChanged();
    }


}