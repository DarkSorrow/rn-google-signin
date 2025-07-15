import React, { useEffect, useState } from 'react';
import { View, Text, Button, Alert } from 'react-native';
import GoogleSignIn from '../src/index';

export default function App() {
  const [isConfigured, setIsConfigured] = useState(false);
  const [user, setUser] = useState<any>(null);
  const [isSignedIn, setIsSignedIn] = useState(false);

  useEffect(() => {
    configureGoogleSignIn();
  }, []);

  const configureGoogleSignIn = async () => {
    try {
      await GoogleSignIn.configure({
        webClientId: 'your-web-client-id.apps.googleusercontent.com', // Replace with your actual client ID
        iosClientId: 'your-ios-client-id.apps.googleusercontent.com', // Replace with your actual iOS client ID
        scopes: ['email', 'profile'],
        offlineAccess: true,
      });
      setIsConfigured(true);
      console.log('✅ Google Sign-In configured successfully');
    } catch (error) {
      console.error('❌ Failed to configure Google Sign-In:', error);
      Alert.alert('Configuration Error', 'Failed to configure Google Sign-In');
    }
  };

  const checkPlayServices = async () => {
    try {
      const hasServices = await GoogleSignIn.hasPlayServices();
      Alert.alert('Play Services', `Available: ${hasServices}`);
    } catch (error) {
      console.error('Play Services check failed:', error);
      Alert.alert('Error', 'Failed to check Play Services');
    }
  };

  const signIn = async () => {
    try {
      const userInfo = await GoogleSignIn.signIn();
      setUser(userInfo);
      setIsSignedIn(true);
      console.log('✅ Sign in successful:', userInfo);
      Alert.alert('Success', 'Signed in successfully!');
    } catch (error) {
      console.error('❌ Sign in failed:', error);
      Alert.alert('Sign In Error', 'Failed to sign in');
    }
  };

  const signOut = async () => {
    try {
      await GoogleSignIn.signOut();
      setUser(null);
      setIsSignedIn(false);
      console.log('✅ Sign out successful');
      Alert.alert('Success', 'Signed out successfully!');
    } catch (error) {
      console.error('❌ Sign out failed:', error);
      Alert.alert('Sign Out Error', 'Failed to sign out');
    }
  };

  const checkSignInStatus = async () => {
    try {
      const signedIn = await GoogleSignIn.isSignedIn();
      setIsSignedIn(signedIn);
      Alert.alert('Sign In Status', `Currently signed in: ${signedIn}`);
    } catch (error) {
      console.error('Status check failed:', error);
      Alert.alert('Error', 'Failed to check sign in status');
    }
  };

  const getCurrentUser = async () => {
    try {
      const currentUser = await GoogleSignIn.getCurrentUser();
      if (currentUser) {
        setUser(currentUser);
        Alert.alert('Current User', `Email: ${currentUser.user.email}`);
      } else {
        Alert.alert('No User', 'No user is currently signed in');
      }
    } catch (error) {
      console.error('Get current user failed:', error);
      Alert.alert('Error', 'Failed to get current user');
    }
  };

  return (
    <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', padding: 20 }}>
      <Text style={{ fontSize: 24, marginBottom: 20 }}>
        React Native Google Sign-In Turbo Module Test
      </Text>
      
      <Text style={{ marginBottom: 20 }}>
        Configuration Status: {isConfigured ? '✅ Configured' : '❌ Not Configured'}
      </Text>
      
      <Text style={{ marginBottom: 20 }}>
        Sign In Status: {isSignedIn ? '✅ Signed In' : '❌ Not Signed In'}
      </Text>

      {user && (
        <View style={{ marginBottom: 20, padding: 10, backgroundColor: '#f0f0f0' }}>
          <Text>Current User:</Text>
          <Text>Email: {user.user.email}</Text>
          <Text>Name: {user.user.name}</Text>
        </View>
      )}

      <Button title="Check Play Services" onPress={checkPlayServices} />
      <View style={{ height: 10 }} />
      
      <Button title="Check Sign In Status" onPress={checkSignInStatus} />
      <View style={{ height: 10 }} />
      
      <Button title="Get Current User" onPress={getCurrentUser} />
      <View style={{ height: 10 }} />
      
      <Button title="Sign In" onPress={signIn} />
      <View style={{ height: 10 }} />
      
      <Button title="Sign Out" onPress={signOut} />
    </View>
  );
} 