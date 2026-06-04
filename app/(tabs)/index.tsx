import { StyleSheet, Text, View } from 'react-native';
import { ScreenScaffold } from '@/ui/ScreenScaffold';
import { colors } from '@/ui/theme';

export default function HomeScreen() {
  return (
    <ScreenScaffold overline="FAMILY DOSE" title="Home">
      <View style={styles.placeholder}>
        <Text style={styles.emoji}>💊</Text>
        <Text style={styles.text}>
          Today&apos;s doses will appear here.{'\n'}Critical, upcoming, and completed.
        </Text>
      </View>
    </ScreenScaffold>
  );
}

const styles = StyleSheet.create({
  placeholder: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: 12 },
  emoji: { fontSize: 48 },
  text: { textAlign: 'center', color: colors.onSurfaceVariant, fontSize: 15, lineHeight: 22 },
});
