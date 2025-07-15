import React, { useEffect, useState } from 'react';
import { View, Text, TouchableOpacity, Alert, StyleSheet } from 'react-native';
import GoogleSignIn, { GoogleSignInUser, statusCodes } from '@novastera-oss/rn-google-signin';

const App = () => {
  const [user, setUser] = useState<GoogleSignInUser | null>(null);
  const [isSigningIn, setIsSigningIn] = useState(false);

  useEffect(() => {
    // Configure Google Sign-In
    const configure = async () => {
      try {
        await GoogleSignIn.configure({
          webClientId: 'YOUR_WEB_CLIENT_ID.apps.googleusercontent.com', // Replace with your client ID
          iosClientId: 'YOUR_IOS_CLIENT_ID.apps.googleusercontent.com', // Replace with your iOS client ID
          offlineAccess: true,
          scopes: ['profile', 'email'],
        });
        
        // Try silent sign-in
        const currentUser = await GoogleSignIn.getCurrentUser();
        setUser(currentUser);
      } catch (error) {
        console.error('Configuration error:', error);
      }
    };

    configure();
  }, []);

  const handleSignIn = async () => {
    setIsSigningIn(true);
    try {
      const hasPlayServices = await GoogleSignIn.hasPlayServices();
      
      if (hasPlayServices) {
        const userInfo = await GoogleSignIn.signIn();
        setUser(userInfo);
      } else {
        Alert.alert('Error', 'Google Play Services not available');
      }
    } catch (error: any) {
      if (error.code === statusCodes.SIGN_IN_CANCELLED) {
        Alert.alert('Sign-in cancelled');
      } else if (error.code === statusCodes.IN_PROGRESS) {
        Alert.alert('Sign-in in progress');
      } else if (error.code === statusCodes.PLAY_SERVICES_NOT_AVAILABLE) {
        Alert.alert('Play Services not available');
      } else {
        Alert.alert('Error', error.message);
      }
    } finally {
      setIsSigningIn(false);
    }
  };

  const handleSignOut = async () => {
    try {
      await GoogleSignIn.signOut();
      setUser(null);
    } catch (error) {
      console.error('Sign-out error:', error);
    }
  };

  const handleGetTokens = async () => {
    try {
      const tokens = await GoogleSignIn.getTokens();
      Alert.alert('Tokens', `Access Token: ${tokens.accessToken.substring(0, 20)}...`);
    } catch (error) {
      Alert.alert('Error', 'Failed to get tokens');
    }
  };

  return (
    <View style={styles.container}>
      {user ? (
        <View style={styles.userContainer}>
          <Text style={styles.title}>Welcome, {user.user.name}!</Text>
          <Text style={styles.email}>{user.user.email}</Text>
          {user.user.photo && (
            <Text style={styles.photo}>Photo: {user.user.photo}</Text>
          )}
          <TouchableOpacity style={styles.button} onPress={handleGetTokens}>
            <Text style={styles.buttonText}>Get Tokens</Text>
          </TouchableOpacity>
          <TouchableOpacity style={[styles.button, styles.signOutButton]} onPress={handleSignOut}>
            <Text style={styles.buttonText}>Sign Out</Text>
          </TouchableOpacity>
        </View>
      ) : (
        <TouchableOpacity
          style={[styles.button, styles.signInButton]}
          onPress={handleSignIn}
          disabled={isSigningIn}
        >
          <Text style={styles.buttonText}>
            {isSigningIn ? 'Signing In...' : 'Sign In with Google'}
          </Text>
        </TouchableOpacity>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
    backgroundColor: '#f5f5f5',
  },
  userContainer: {
    alignItems: 'center',
    backgroundColor: 'white',
    padding: 20,
    borderRadius: 10,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.25,
    shadowRadius: 3.84,
    elevation: 5,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 10,
    color: '#333',
  },
  email: {
    fontSize: 16,
    color: '#666',
    marginBottom: 10,
  },
  photo: {
    fontSize: 14,
    color: '#999',
    marginBottom: 20,
  },
  button: {
    paddingHorizontal: 20,
    paddingVertical: 12,
    borderRadius: 8,
    marginVertical: 5,
    minWidth: 200,
    alignItems: 'center',
  },
  signInButton: {
    backgroundColor: '#4285f4',
  },
  signOutButton: {
    backgroundColor: '#db4437',
  },
  buttonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: '600',
  },
});

export default App; 