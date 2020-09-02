package lib.folderpicker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class FolderPicker extends Activity {

    Comparator<FilePojo> comparatorAscending = new Comparator<FilePojo>() {
        @Override
        public int compare(FilePojo f1, FilePojo f2) {
            return f1.getName().compareTo(f2.getName());
        }
    };

    public static final String EXTRA_DATA = "data";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_LOCATION = "location";
    public static final String EXTRA_PICK_FILES = "pickFiles";
    //Folders and Files have separate lists because we show all folders first then files
    ArrayList<FilePojo> mFolderAndFileList;
    ArrayList<FilePojo> mFoldersList;
    ArrayList<FilePojo> mFilesList;

    TextView mTvTitle;
    TextView mTvLocation;

    String mLocation = Environment.getExternalStorageDirectory().getAbsolutePath();
    boolean mPickFiles;
    Intent mReceivedIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fp_main_layout);

        if (!isExternalStorageReadable()) {
            Toast.makeText(this, getString(R.string.no_access_to_storage), Toast.LENGTH_LONG).show();
            finish();
        }

        mTvTitle = findViewById(R.id.fp_tv_title);
        mTvLocation = findViewById(R.id.fp_tv_location);

        try {
            mReceivedIntent = getIntent();

            if (mReceivedIntent.hasExtra(EXTRA_TITLE)) {
                String receivedTitle = mReceivedIntent.getStringExtra(EXTRA_TITLE);
                if (receivedTitle != null) {
                    mTvTitle.setText(receivedTitle);
                }
            }

            if (mReceivedIntent.hasExtra(EXTRA_LOCATION)) {
                String reqLocation = mReceivedIntent.getStringExtra(EXTRA_LOCATION);
                if (reqLocation != null) {
                    File requestedFolder = new File(reqLocation);
                    if (requestedFolder.exists())
                        mLocation = reqLocation;
                }
            }

            if (mReceivedIntent.hasExtra(EXTRA_PICK_FILES)) {
                mPickFiles = mReceivedIntent.getBooleanExtra(EXTRA_PICK_FILES, false);
                if (mPickFiles) {
                    findViewById(R.id.fp_btn_select).setVisibility(View.GONE);
                    findViewById(R.id.fp_btn_new).setVisibility(View.GONE);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        loadLists(mLocation);
    }

    /* Checks if external storage is available to at least read */
    boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    void loadLists(String location) {
        try {
            File folder = new File(location);

            if (!folder.isDirectory())
                exit();

            mTvLocation.setText(String.format(getString(R.string.location_mask), folder.getAbsolutePath()));
            File[] files = folder.listFiles();

            mFoldersList = new ArrayList<>();
            mFilesList = new ArrayList<>();

            for (File currentFile : files) {
                if (currentFile.isDirectory()) {
                    FilePojo filePojo = new FilePojo(currentFile.getName(), true);
                    mFoldersList.add(filePojo);
                } else {
                    FilePojo filePojo = new FilePojo(currentFile.getName(), false);
                    mFilesList.add(filePojo);
                }
            }

            // sort & add to final List - as we show folders first add folders first to the final list
            Collections.sort(mFoldersList, comparatorAscending);
            mFolderAndFileList = new ArrayList<>();
            mFolderAndFileList.addAll(mFoldersList);

            //if we have to show files, then add files also to the final list
            if (mPickFiles) {
                Collections.sort(mFilesList, comparatorAscending );
                mFolderAndFileList.addAll(mFilesList);
            }

            showList();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    void showList() {
        try {
            FolderAdapter FolderAdapter = new FolderAdapter(this, mFolderAndFileList);
            ListView listView = (ListView) findViewById(R.id.fp_listView);
            listView.setAdapter(FolderAdapter);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    listClick(position);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    void listClick(int position) {
        if (mPickFiles && !mFolderAndFileList.get(position).isFolder()) {
            String data = mLocation + File.separator + mFolderAndFileList.get(position).getName();
            mReceivedIntent.putExtra(EXTRA_DATA, data);
            setResult(RESULT_OK, mReceivedIntent);
            finish();
        } else {
            mLocation = mLocation + File.separator + mFolderAndFileList.get(position).getName();
            loadLists(mLocation);
        }
    }

    @Override
    public void onBackPressed(){
        goBack(null);
    }

    public void goBack(View v) {
        if (mLocation != null && !mLocation.equals("") && !mLocation.equals("/")) {
            int start = mLocation.lastIndexOf('/');
            String newLocation = mLocation.substring(0, start);
            mLocation = newLocation;
            loadLists(mLocation);
        } else {
            exit();
        }
    }

    void exit() {
        setResult(RESULT_CANCELED, mReceivedIntent);
        finish();
    }

    void createNewFolder(String filename) {
        try {
            File file = new File(mLocation + File.separator + filename);
            file.mkdirs();
            loadLists(mLocation);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, String.format(getString(R.string.error_string_mask), e.toString()), Toast.LENGTH_LONG)
                    .show();
        }
    }

    public void newFolderDialog(View v) {
        LayoutInflater inflater = LayoutInflater.from(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = inflater.inflate(R.layout.dialog_folder_name, null);
        builder.setView(view);
        builder.setTitle(getString(R.string.enter_folder_name));

        final EditText et = view.findViewById(R.id.edit_text);

        final AlertDialog dialog = builder.create();
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.create),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        createNewFolder(et.getText().toString());
                    }
                });
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel), (DialogInterface.OnClickListener)null);

        dialog.show();
    }

    public void select(View v) {

        if (mPickFiles) {
            Toast.makeText(this, getString(R.string.select_file), Toast.LENGTH_LONG).show();
        } else if (mReceivedIntent != null) {
            mReceivedIntent.putExtra(EXTRA_DATA, mLocation);
            setResult(RESULT_OK, mReceivedIntent);
            finish();
        }
    }

    public void cancel(View v) {
        exit();
    }

}
