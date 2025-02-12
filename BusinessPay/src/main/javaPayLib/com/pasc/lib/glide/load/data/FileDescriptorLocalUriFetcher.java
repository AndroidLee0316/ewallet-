package com.pasc.lib.glide.load.data;

import android.content.ContentResolver;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Fetches an {@link ParcelFileDescriptor} for a local {@link Uri}.
 */
public class FileDescriptorLocalUriFetcher extends LocalUriFetcher<ParcelFileDescriptor> {
  public FileDescriptorLocalUriFetcher(ContentResolver contentResolver, Uri uri) {
    super(contentResolver, uri);
  }

  @Override
  protected ParcelFileDescriptor loadResource(Uri uri, ContentResolver contentResolver)
      throws FileNotFoundException {
    AssetFileDescriptor assetFileDescriptor = contentResolver.openAssetFileDescriptor(uri, "r");
    if (assetFileDescriptor == null) {
      throw new FileNotFoundException("FileDescriptor is null for: " + uri);
    }
    return assetFileDescriptor.getParcelFileDescriptor();
  }

  @Override
  protected void close(ParcelFileDescriptor data) throws IOException {
    data.close();
  }

  @NonNull
  @Override
  public Class<ParcelFileDescriptor> getDataClass() {
    return ParcelFileDescriptor.class;
  }
}
