import { StyleSheet, Text, View } from 'react-native';
import { ScreenScaffold } from '@/ui/ScreenScaffold';
import { colors } from '@/ui/theme';

export default function AccountScreen() {
  return (
    <ScreenScaffold overline="ACCOUNT" title="History & Sharing">
      <View style={styles.placeholder}>
        <Text style={styles.emoji}>👤</Text>
        <Text style={styles.text}>Dose history and caretaker access will appear here.</Text>
      </View>
    </ScreenScaffold>
  );
}

const styles = StyleSheet.create({
  placeholder: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: 12 },
  emoji: { fontSize: 48 },
  text: { textAlign: 'center', color: colors.onSurfaceVariant, fontSize: 15 },
});
