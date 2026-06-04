import { ReactNode } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { colors, spacing } from '@/ui/theme';

/** Shared screen wrapper: safe-area, background, an overline label + title header. */
export function ScreenScaffold({
  overline,
  title,
  children,
}: {
  overline: string;
  title: string;
  children?: ReactNode;
}) {
  return (
    <SafeAreaView style={styles.safe} edges={['top', 'left', 'right']}>
      <View style={styles.header}>
        <Text style={styles.overline}>{overline}</Text>
        <Text style={styles.title}>{title}</Text>
      </View>
      <View style={styles.body}>{children}</View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  header: { paddingHorizontal: spacing.md, paddingTop: spacing.lg, paddingBottom: spacing.sm },
  overline: {
    fontSize: 11,
    fontWeight: '700',
    letterSpacing: 1,
    color: colors.onSurfaceVariant,
  },
  title: { fontSize: 22, fontWeight: '600', color: colors.onSurface },
  body: { flex: 1, paddingHorizontal: spacing.md },
});
