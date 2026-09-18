import * as fs from 'fs';
import * as path from 'path';
import * as vscode from 'vscode';
import { spawn } from 'child_process';

const IS_WINDOWS = process.platform === 'win32';

export interface CliResult {
    code: number;
    stdout: string;
    stderr: string;
}

/**
 * CLI 실행 파일을 찾는다. 우선순위:
 * 1. programmers.cliPath 설정
 * 2. 워크스페이스의 programmers-cli/build/install/programmers/bin (이 저장소를 직접 여는 개발 흐름)
 * 3. PATH
 */
export function findCli(workspaceRoot: string | undefined): string | undefined {
    const configured = vscode.workspace.getConfiguration('programmers').get<string>('cliPath');
    if (configured && configured.trim().length > 0) {
        return configured.trim();
    }

    if (workspaceRoot) {
        const installed = path.join(
            workspaceRoot,
            'programmers-cli', 'build', 'install', 'programmers', 'bin',
            IS_WINDOWS ? 'programmers.bat' : 'programmers'
        );
        if (fs.existsSync(installed)) {
            return installed;
        }
    }

    return findOnPath(IS_WINDOWS ? 'programmers.bat' : 'programmers');
}

function findOnPath(executable: string): string | undefined {
    const pathEnv = process.env.PATH ?? '';
    for (const dir of pathEnv.split(path.delimiter)) {
        if (!dir) continue;
        const candidate = path.join(dir, executable);
        if (fs.existsSync(candidate)) {
            return candidate;
        }
    }
    return undefined;
}

export function runImport(cliPath: string, url: string, outputPath: string, force: boolean): Promise<CliResult> {
    const args = ['import', url, '--output', outputPath];
    if (force) {
        args.push('--force');
    }
    return runProcess(cliPath, args);
}

export async function buildCliWithGradle(workspaceRoot: string): Promise<boolean> {
    const gradlew = path.join(workspaceRoot, IS_WINDOWS ? 'gradlew.bat' : 'gradlew');
    if (!fs.existsSync(gradlew)) {
        vscode.window.showErrorMessage('gradlew를 찾을 수 없어 programmers-cli를 빌드할 수 없습니다.');
        return false;
    }

    return vscode.window.withProgress(
        { location: vscode.ProgressLocation.Notification, title: 'programmers-cli 빌드 중 (Gradle)...' },
        async () => {
            const result = await runProcess(gradlew, [':programmers-cli:installDist'], workspaceRoot);
            if (result.code !== 0) {
                vscode.window.showErrorMessage(
                    `programmers-cli 빌드에 실패했습니다.\n${result.stderr.trim().slice(-500)}`
                );
                return false;
            }
            return true;
        }
    );
}

function runProcess(command: string, args: string[], cwd?: string): Promise<CliResult> {
    return new Promise((resolve, reject) => {
        const child = spawn(command, args, { cwd, shell: IS_WINDOWS });
        let stdout = '';
        let stderr = '';
        child.stdout?.on('data', (chunk) => { stdout += chunk.toString(); });
        child.stderr?.on('data', (chunk) => { stderr += chunk.toString(); });
        child.on('error', reject);
        child.on('close', (code) => resolve({ code: code ?? -1, stdout, stderr }));
    });
}
