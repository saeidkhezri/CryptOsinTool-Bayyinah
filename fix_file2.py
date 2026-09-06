import sys

def main():
    with open("app/src/main/java/com/aistudio/orbit/ui/InvestigationViewModel.kt", "r") as f:
        lines = f.readlines()
        
    patch = []
    with open("patch_investigation.kt", "r") as f:
        patch = f.readlines()
        
    out = []
    i = 0
    while i < len(lines):
        line = lines[i]
        
        # Check if this is the target area
        if "Querying public blockchain ledger..." in line:
            out.append(line)
            # The next line is try {
            out.append(lines[i+1])
            # The line after that is the orphaned }
            if "}" in lines[i+2]:
                # Skip the }
                pass
            out.extend(patch)
            i += 2
        else:
            out.append(line)
        i += 1
        
    with open("app/src/main/java/com/aistudio/orbit/ui/InvestigationViewModel.kt", "w") as f:
        f.writelines(out)

if __name__ == "__main__":
    main()
