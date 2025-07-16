import { useState, useEffect } from 'react';
import { Text, View, StyleSheet, TouchableOpacity, Alert } from 'react-native';
import RnGoogleSignin from '@novastera-oss/rn-google-signin';
import type { GoogleSignInErrorCode } from '@novastera-oss/rn-google-signin';

export default function App() {
  const [isSignedIn, setIsSignedIn] = useState(false);
  const [user, setUser] = useState<any>(null);

  useEffect(() => {
    // Configure Google Sign-In (synchronous)
    try {
      RnGoogleSignin.configure({
        iosClientId: 'YOUR_IOS_CLIENT_ID', // Replace with your iOS client ID
        webClientId: 'YOUR_WEB_CLIENT_ID', // Replace with your web client ID
        scopes: ['profile', 'email']
      });

      // Check if user is already signed in
      checkSignInStatus();
    } catch (error) {
      console.error('Configuration error:', error);
      Alert.alert('Error', 'Failed to configure Google Sign-In');
    }
  }, []);

  const checkSignInStatus = async () => {
    try {
      const signedIn = await RnGoogleSignin.isSignedIn();
      setIsSignedIn(signedIn);
      
      if (signedIn) {
        const currentUser = await RnGoogleSignin.getCurrentUser();
        setUser(currentUser);
      }
    } catch (error) {
      console.error('Error checking sign in status:', error);
    }
  };

  const signIn = async () => {
    try {
      const result = await RnGoogleSignin.signIn(null);
      setUser(result.user);
      setIsSignedIn(true);
      Alert.alert('Success', 'Signed in successfully!');
    } catch (error: any) {
      console.error('Sign in error:', error);
      const errorCode = error.code as GoogleSignInErrorCode;
      switch (errorCode) {
        case 'sign_in_cancelled':
          Alert.alert('Cancelled', 'Sign in was cancelled');
          break;
        case 'not_configured':
          Alert.alert('Error', 'Google Sign In is not configured');
          break;
        case 'play_services_not_available':
          Alert.alert('Error', 'Google Play Services not available');
          break;
        default:
          Alert.alert('Error', error.message || 'Sign in failed');
          break;
      }
    }
  };

  const signOut = async () => {
    try {
      await RnGoogleSignin.signOut();
      setUser(null);
      setIsSignedIn(false);
      Alert.alert('Success', 'Signed out successfully!');
    } catch (error: any) {
      console.error('Sign out error:', error);
      Alert.alert('Error', error.message || 'Sign out failed');
    }
  };

  const getTokens = async () => {
    try {
      const tokens = await RnGoogleSignin.getTokens();
      Alert.alert('Tokens', `Access Token: ${tokens.accessToken.substring(0, 20)}...`);
    } catch (error: any) {
      console.error('Get tokens error:', error);
      Alert.alert('Error', error.message || 'Failed to get tokens');
    }
  };

  const checkPlayServices = async () => {
    try {
      const hasServices = await RnGoogleSignin.hasPlayServices(null);
      Alert.alert('Play Services', hasServices ? 'Available' : 'Not available');
    } catch (error: any) {
      console.error('Play services check error:', error);
      Alert.alert('Error', error.message || 'Failed to check Play Services');
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Google Sign-In Turbo Module</Text>
      
      {!isSignedIn ? (
        <TouchableOpacity style={styles.button} onPress={signIn}>
          <Text style={styles.buttonText}>Sign In with Google</Text>
        </TouchableOpacity>
      ) : (
        <View style={styles.userContainer}>
          <Text style={styles.userText}>Welcome, {user?.name}!</Text>
          <Text style={styles.userText}>Email: {user?.email}</Text>
          
          <TouchableOpacity style={styles.button} onPress={getTokens}>
            <Text style={styles.buttonText}>Get Tokens</Text>
          </TouchableOpacity>
          
          <TouchableOpacity style={styles.button} onPress={checkPlayServices}>
            <Text style={styles.buttonText}>Check Play Services</Text>
          </TouchableOpacity>
          
          <TouchableOpacity style={[styles.button, styles.signOutButton]} onPress={signOut}>
            <Text style={styles.buttonText}>Sign Out</Text>
          </TouchableOpacity>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 20,
    backgroundColor: '#f5f5f5',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 30,
    textAlign: 'center',
  },
  button: {
    backgroundColor: '#4285f4',
    paddingHorizontal: 30,
    paddingVertical: 15,
    borderRadius: 8,
    marginVertical: 10,
  },
  buttonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: '600',
  },
  signOutButton: {
    backgroundColor: '#db4437',
  },
  userContainer: {
    alignItems: 'center',
  },
  userText: {
    fontSize: 16,
    marginVertical: 5,
    textAlign: 'center',
  },
});
