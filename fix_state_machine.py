import re

with open("app/src/main/java/com/aistudio/orbit/model/InvestigationStateMachine.kt", "r") as f:
    content = f.read()

# Replace all occurrences of `recommendedNextAction = "..."`
# with `recommendedNextAction = "...", nextBestActionDetailed = NextBestAction(action="...", reason="...", supportingEvidenceCount=evidenceCount, expectedInvestigativeValue="High", requiredInput="None", targetStage="Next", confidence=confidence, costImplication="Standard API limits")`

def replacer(match):
    action_text = match.group(1)
    return f'''recommendedNextAction = "{action_text}",
                nextBestActionDetailed = NextBestAction(
                    action = "{action_text}",
                    reason = "Derived from current state progression",
                    supportingEvidenceCount = evidenceCount,
                    expectedInvestigativeValue = "High",
                    requiredInput = "Target Address",
                    targetStage = "Next available phase",
                    confidence = confidence,
                    costImplication = "Standard API limits"
                )'''

content = re.sub(r'recommendedNextAction\s*=\s*"([^"]+)"', replacer, content)

with open("app/src/main/java/com/aistudio/orbit/model/InvestigationStateMachine.kt", "w") as f:
    f.write(content)
