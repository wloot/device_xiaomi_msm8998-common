package android.security.keystore;

import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.KeyStoreException;

import android.security.Credentials;
import android.security.KeyStore;
import android.security.keystore.AndroidKeyStoreProvider;

/**
 * Created by henryye on 15/10/13.
 * If need any edit necessary, please contact him by RTX
 * @hide
 */
public class SoterKeyStoreSpi extends android.security.keystore.AndroidKeyStoreSpi{

    private KeyStore mKeyStore = null;

    public SoterKeyStoreSpi(){
        mKeyStore = KeyStore.getInstance();//chenlingyun move init mKeyStore to here
    }
    
    @Override
    public Key engineGetKey(String alias, char[] password) throws NoSuchAlgorithmException,
            UnrecoverableKeyException {
        String userKeyAlias = Credentials.USER_PRIVATE_KEY + alias;
        if (!mKeyStore.contains(userKeyAlias)) {
         if(password != null && "from_soter_ui".equals(String.valueOf(password)))
            {
                return SoterKeyStoreProvider.loadJsonPublicKeyFromKeystore(
                    mKeyStore, userKeyAlias);
            }
            // try legacy prefix for backward compatibility
            userKeyAlias = Credentials.USER_SECRET_KEY + alias;
            if (!mKeyStore.contains(userKeyAlias)) return null;
        }
        return AndroidKeyStoreProvider.loadAndroidKeyStoreKeyFromKeystore(mKeyStore, userKeyAlias,
                    KeyStore.UID_SELF);
    }

    @Override
    public void engineDeleteEntry(String alias) throws KeyStoreException
    {
        if (!engineContainsAlias(alias))
        {
            return;
        }
        // At least one entry corresponding to this alias exists in keystore

        if (!(mKeyStore.delete(Credentials.USER_PRIVATE_KEY + alias) | mKeyStore.delete(Credentials.USER_CERTIFICATE + alias)))
        {
            throw new KeyStoreException("Failed to delete entry: " + alias);
        }
    }

    @Override
    public boolean engineContainsAlias(String alias)
    {
        if (alias == null)
        {
            throw new NullPointerException("alias == null");
        }

        return mKeyStore.contains(Credentials.USER_PRIVATE_KEY + alias) ||
                mKeyStore.contains(Credentials.USER_CERTIFICATE + alias);
    }
}