                        // Quick Actions
                        @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(ForensicSpacing.sm),
                            verticalArrangement = Arrangement.spacedBy(ForensicSpacing.sm)
                        ) {
                            Button(
                                onClick = onNavigateToNew,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = ForensicShapes.md,
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .heightIn(min = ForensicTouchTarget.minSize)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(ForensicSpacing.xs))
                                Text(
                                    text = strings.tabNewInvestigation,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Button(
                                onClick = onNavigateToOsint,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF795548), // Earthy Brown/Bronze
                                    contentColor = Color.White
                                ),
                                shape = ForensicShapes.md,
                                contentPadding = PaddingValues(horizontal = ForensicSpacing.md, vertical = 0.dp),
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .heightIn(min = ForensicTouchTarget.minSize)
                            ) {
                                Icon(Icons.Default.TravelExplore, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(ForensicSpacing.xs))
                                Text(
                                    text = if (isPersian) "سامانه OSINT" else "OSINT Hub",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
