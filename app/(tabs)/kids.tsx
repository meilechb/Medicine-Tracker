import { StyleSheet, Text, View } from 'react-native';
import { ScreenScaffold } from '@/ui/ScreenScaffold';
import { colors } from '@/ui/theme';

export default function KidsScreen() {
  return (
    <ScreenScaffold overline="KIDS PROFILES" title="Children & Shared Access">
      <View style={styles.placeholder}>
        <Text style={styles.emoji}>🧒</Text>
        <Text style={styles.text}>Children profiles will appear here.</Text>
      </View>
    </ScreenScaffold>
  );
}

const styles = StyleSheet.create({
  placeholder: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: 12 },
  emoji: { fontSize: 48 },
  text: { textAlign: 'center', color: colors.onSurfaceVariant, fontSize: 15 },
});
